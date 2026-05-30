import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { Plus, Pencil, ArrowRight } from 'lucide-react';

const API = 'http://localhost:8080/api/v1';

function ProjectManagement() {
  const [projects, setProjects] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingProject, setEditingProject] = useState(null);
  const [formData, setFormData] = useState({ name: '', description: '' });
  const [msg, setMsg] = useState({ text: '', type: '' });
  const navigate = useNavigate();

  const userStr = localStorage.getItem('user');
  const user = userStr ? JSON.parse(userStr) : null;
  const role = user?.role || 'MEMBER';
  const canManage = role === 'ADMIN' || role === 'MANAGER';

  useEffect(() => { fetchProjects(); }, []);

  const fetchProjects = async () => {
    try {
      const res = await axios.get(`${API}/projects`);
      setProjects(res.data.content || []);
    } catch (err) {
      setMsg({ text: 'Failed to load projects.', type: 'error' });
    }
  };

  const openCreateForm = () => {
    setEditingProject(null);
    setFormData({ name: '', description: '' });
    setShowForm(true);
    setMsg({ text: '', type: '' });
  };

  const openEditForm = (p) => {
    setEditingProject(p);
    setFormData({ name: p.name, description: p.description || '' });
    setShowForm(true);
    setMsg({ text: '', type: '' });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingProject) {
        await axios.put(`${API}/projects/${editingProject.id}`, formData);
        setMsg({ text: `Project "${formData.name}" updated!`, type: 'success' });
      } else {
        const res = await axios.post(`${API}/projects`, formData);
        setMsg({ text: `Project "${formData.name}" created!`, type: 'success' });
      }
      setShowForm(false);
      setEditingProject(null);
      fetchProjects();
    } catch (err) {
      const detail = err.response?.data?.message || 'Failed to save project.';
      setMsg({ text: detail, type: 'error' });
    }
  };

  const handleDelete = async (p) => {
    if (!window.confirm(`Delete project "${p.name}"? This cannot be undone.`)) return;
    try {
      await axios.delete(`${API}/projects/${p.id}`);
      setMsg({ text: `Project "${p.name}" deleted.`, type: 'success' });
      fetchProjects();
    } catch (err) {
      setMsg({ text: 'Failed to delete project.', type: 'error' });
    }
  };

  return (
    <div>
      <Navbar />
      <div className="page-container">
        <div className="page-header">
          <h2>{canManage ? 'Manage Projects' : 'Projects'}</h2>
          {canManage && !showForm && (
            <button className="btn btn-primary" onClick={openCreateForm}><Plus size={16} /> Create Project</button>
          )}
        </div>

        {msg.text && <div className={msg.type === 'success' ? 'msg-success' : 'msg-error'}>{msg.text}</div>}

        {/* Create / Edit Form */}
        {showForm && (
          <div className="form-card" style={{ marginBottom: '24px' }}>
            <h3>{editingProject ? `Edit Project — ${editingProject.name}` : 'Create New Project'}</h3>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Project Name *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g. Sprint 1"
                  required
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={formData.description}
                  onChange={e => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Brief description of this project..."
                />
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary">{editingProject ? 'Update' : 'Create'}</button>
                <button type="button" className="btn btn-secondary" onClick={() => { setShowForm(false); setEditingProject(null); }}>Cancel</button>
              </div>
            </form>
          </div>
        )}

        {/* Projects List */}
        {projects.length === 0 && !showForm && (
          <div className="empty-state">
            No projects found. {canManage ? 'Create one to get started!' : 'Wait for an admin to create a project.'}
          </div>
        )}

        {projects.map(p => (
          <div key={p.id} className="project-list-item">
            <div onClick={() => navigate(`/projects/${p.id}`)} style={{ flex: 1, cursor: 'pointer' }}>
              <h4>{p.name}</h4>
              <p>{p.description || 'No description'}</p>
            </div>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              {canManage && (
                <button className="btn btn-outline btn-sm" onClick={(e) => { e.stopPropagation(); openEditForm(p); }}>
                  <Pencil size={14} /> Edit
                </button>
              )}
              {canManage && (
                <button className="btn btn-danger btn-sm" onClick={(e) => { e.stopPropagation(); handleDelete(p); }}>
                  Delete
                </button>
              )}
              <button className="btn btn-primary btn-sm" onClick={() => navigate(`/projects/${p.id}`)}>
                <ArrowRight size={14} /> Open Board
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default ProjectManagement;
