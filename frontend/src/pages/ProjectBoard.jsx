import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useParams } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { Plus, X, Pencil } from 'lucide-react';

const API = 'http://localhost:8080/api/v1';
const COLUMNS = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const COLUMN_LABELS = { TODO: 'To Do', IN_PROGRESS: 'In Progress', IN_REVIEW: 'In Review', DONE: 'Done' };

function ProjectBoard() {
  const { projectId } = useParams();
  const [tasks, setTasks] = useState([]);
  const [users, setUsers] = useState([]);
  const [draggingTaskId, setDraggingTaskId] = useState(null);
  const [showTaskForm, setShowTaskForm] = useState(false);
  const [editingTask, setEditingTask] = useState(null);
  const [msg, setMsg] = useState({ text: '', type: '' });
  const [taskForm, setTaskForm] = useState({
    title: '', description: '', priority: 'MEDIUM', status: 'TODO', assigneeId: '', dueDate: ''
  });

  const userStr = localStorage.getItem('user');
  const user = userStr ? JSON.parse(userStr) : null;
  const role = user?.role || 'MEMBER';
  const canManage = role === 'ADMIN' || role === 'MANAGER';

  useEffect(() => {
    fetchTasks();
    if (canManage) fetchUsers();
  }, [projectId]);

  const fetchTasks = async () => {
    try {
      const res = await axios.get(`${API}/tasks?projectId=${projectId}`);
      setTasks(res.data.content || []);
    } catch (err) { console.error(err); }
  };

  const fetchUsers = async () => {
    try {
      const res = await axios.get(`${API}/users`);
      setUsers(res.data);
    } catch (err) { console.error(err); }
  };

  // ─── Task Form Handlers ───
  const openCreateForm = () => {
    setEditingTask(null);
    setTaskForm({ title: '', description: '', priority: 'MEDIUM', status: 'TODO', assigneeId: '', dueDate: '' });
    setShowTaskForm(true);
    setMsg({ text: '', type: '' });
  };

  const openEditForm = (task) => {
    setEditingTask(task);
    setTaskForm({
      title: task.title || '',
      description: task.description || '',
      priority: task.priority || 'MEDIUM',
      status: task.status || 'TODO',
      assigneeId: task.assigneeId || '',
      dueDate: task.dueDate || ''
    });
    setShowTaskForm(true);
    setMsg({ text: '', type: '' });
  };

  const handleTaskSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      projectId: projectId,
      title: taskForm.title,
      description: taskForm.description || null,
      priority: taskForm.priority,
      status: taskForm.status,
      assigneeId: taskForm.assigneeId || null,
      dueDate: taskForm.dueDate || null
    };
    try {
      if (editingTask) {
        await axios.put(`${API}/tasks/${editingTask.id}`, payload);
        setMsg({ text: `Task "${taskForm.title}" updated!`, type: 'success' });
      } else {
        await axios.post(`${API}/tasks`, payload);
        setMsg({ text: `Task "${taskForm.title}" created!`, type: 'success' });
      }
      setShowTaskForm(false);
      setEditingTask(null);
      fetchTasks();
    } catch (err) {
      const detail = err.response?.data?.message || 'Failed to save task.';
      setMsg({ text: detail, type: 'error' });
    }
  };

  const handleDeleteTask = async (task) => {
    if (!window.confirm(`Delete task "${task.title}"?`)) return;
    try {
      await axios.delete(`${API}/tasks/${task.id}`);
      setMsg({ text: `Task deleted.`, type: 'success' });
      fetchTasks();
    } catch (err) {
      setMsg({ text: 'Failed to delete task.', type: 'error' });
    }
  };

  // ─── Drag & Drop ───
  const updateTaskStatus = async (taskId, newStatus) => {
    const taskToUpdate = tasks.find(t => t.id === taskId);
    if (!taskToUpdate) return;
    setTasks(prev => prev.map(t => t.id === taskId ? { ...t, status: newStatus } : t));
    try {
      await axios.put(`${API}/tasks/${taskId}`, {
        projectId, title: taskToUpdate.title, description: taskToUpdate.description,
        priority: taskToUpdate.priority, status: newStatus,
        dueDate: taskToUpdate.dueDate, assigneeId: taskToUpdate.assigneeId
      });
    } catch (err) {
      setMsg({ text: 'Failed to update status. You may not have permission.', type: 'error' });
      fetchTasks();
    }
  };

  const handleDragStart = (e, task) => {
    if (role === 'MEMBER' && task.assigneeId !== user?.id) { e.preventDefault(); return; }
    setDraggingTaskId(task.id);
    e.dataTransfer.setData('text/plain', task.id);
    e.currentTarget.style.opacity = '0.5';
  };

  const handleDragEnd = (e) => {
    e.currentTarget.style.opacity = '1';
    setDraggingTaskId(null);
  };

  const handleDragOver = (e) => e.preventDefault();

  const handleDrop = (e, targetStatus) => {
    e.preventDefault();
    const taskId = e.dataTransfer.getData('text');
    if (taskId && draggingTaskId) {
      const task = tasks.find(t => t.id === taskId);
      if (task && task.status !== targetStatus) updateTaskStatus(taskId, targetStatus);
    }
  };

  const getUserName = (id) => {
    const u = users.find(u => u.id === id);
    return u ? u.name : '';
  };

  return (
    <div>
      <Navbar />

      {/* Task Form Modal */}
      {showTaskForm && (
        <div className="modal-overlay" onClick={() => setShowTaskForm(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0 }}>{editingTask ? `Edit Task` : 'Create New Task'}</h3>
              <button className="btn btn-secondary btn-sm" onClick={() => setShowTaskForm(false)}><X size={16} /></button>
            </div>

            <form onSubmit={handleTaskSubmit}>
              <div className="form-group">
                <label>Title *</label>
                <input type="text" value={taskForm.title} onChange={e => setTaskForm({ ...taskForm, title: e.target.value })} placeholder="Task title" required />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea value={taskForm.description} onChange={e => setTaskForm({ ...taskForm, description: e.target.value })} placeholder="Describe the task..." />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="form-group">
                  <label>Priority *</label>
                  <select value={taskForm.priority} onChange={e => setTaskForm({ ...taskForm, priority: e.target.value })}>
                    {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label>Status</label>
                  <select value={taskForm.status} onChange={e => setTaskForm({ ...taskForm, status: e.target.value })}>
                    {COLUMNS.map(s => <option key={s} value={s}>{COLUMN_LABELS[s]}</option>)}
                  </select>
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <div className="form-group">
                  <label>Assignee</label>
                  <select value={taskForm.assigneeId} onChange={e => setTaskForm({ ...taskForm, assigneeId: e.target.value })}>
                    <option value="">Unassigned</option>
                    {users.map(u => <option key={u.id} value={u.id}>{u.name} ({u.role})</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label>Due Date</label>
                  <input type="date" value={taskForm.dueDate} onChange={e => setTaskForm({ ...taskForm, dueDate: e.target.value })} />
                  <p className="form-info">Must be a future date</p>
                </div>
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary">{editingTask ? 'Update Task' : 'Create Task'}</button>
                <button type="button" className="btn btn-secondary" onClick={() => setShowTaskForm(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="board-container">
        <div className="board-header">
          <h2>Project Board</h2>
          <div style={{ display: 'flex', gap: '10px' }}>
            {canManage && (
              <button className="btn btn-primary" onClick={openCreateForm}><Plus size={16} /> Create Task</button>
            )}
          </div>
        </div>

        {msg.text && <div className={msg.type === 'success' ? 'msg-success' : 'msg-error'}>{msg.text}</div>}

        <div className="columns">
          {COLUMNS.map(col => {
            const colTasks = tasks.filter(t => t.status === col);
            return (
              <div key={col} className="column" onDragOver={handleDragOver} onDrop={(e) => handleDrop(e, col)}>
                <div className="column-header">
                  {COLUMN_LABELS[col]}
                  <span className="column-count">{colTasks.length}</span>
                </div>
                <div className="task-list">
                  {colTasks.map(task => (
                    <div
                      key={task.id}
                      className="task-card"
                      draggable={canManage || task.assigneeId === user?.id}
                      onDragStart={(e) => handleDragStart(e, task)}
                      onDragEnd={handleDragEnd}
                      style={{
                        opacity: (role === 'MEMBER' && task.assigneeId !== user?.id) ? 0.6 : 1,
                        cursor: (role === 'MEMBER' && task.assigneeId !== user?.id) ? 'default' : 'grab'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div className="task-title">{task.title}</div>
                        {canManage && (
                          <div style={{ display: 'flex', gap: '4px', flexShrink: 0 }}>
                            <button onClick={() => openEditForm(task)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#5e6c84', padding: '2px' }} title="Edit"><Pencil size={14} /></button>
                            <button onClick={() => handleDeleteTask(task)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#de350b', padding: '2px' }} title="Delete"><X size={14} /></button>
                          </div>
                        )}
                      </div>
                      {task.description && <div style={{ fontSize: '0.8rem', color: '#5e6c84', marginBottom: '8px' }}>{task.description}</div>}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span className={`task-priority ${task.priority}`}>{task.priority}</span>
                        <div style={{ display: 'flex', gap: '8px', alignItems: 'center', fontSize: '0.75rem', color: '#5e6c84' }}>
                          {task.assigneeId && <span title="Assignee">{getUserName(task.assigneeId) || 'Assigned'}</span>}
                          {task.dueDate && <span>{new Date(task.dueDate).toLocaleDateString()}</span>}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default ProjectBoard;
