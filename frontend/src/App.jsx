import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { LayoutDashboard, LogOut } from 'lucide-react';
import './index.css';

const API_URL = 'http://localhost:8080/api/v1';

function App() {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [tasks, setTasks] = useState([]);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [isLogin, setIsLogin] = useState(true);
  const [draggingTaskId, setDraggingTaskId] = useState(null);

  useEffect(() => {
    axios.interceptors.request.use(config => {
      if (token) config.headers.Authorization = `Bearer ${token}`;
      return config;
    });
  }, [token]);

  useEffect(() => {
    if (token) fetchTasks();
  }, [token]);

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post(`${API_URL}/auth/login`, { email, password });
      setToken(res.data.accessToken);
      localStorage.setItem('token', res.data.accessToken);
    } catch (err) {
      alert("Login failed! Please check credentials or register first.");
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`${API_URL}/auth/register`, {
        name,
        email,
        password,
        organizationId: '11111111-1111-1111-1111-111111111111' // Sample org from V3 migration
      });
      alert('Registration successful! You can now log in.');
      setIsLogin(true);
    } catch (err) {
      alert("Registration failed! Email might already be in use.");
    }
  };

  const handleLogout = () => {
    setToken(null);
    localStorage.removeItem('token');
  };

  const fetchTasks = async () => {
    try {
      const res = await axios.get(`${API_URL}/tasks`);
      setTasks(res.data.content || []);
    } catch (err) {
      console.error(err);
    }
  };

  const updateTaskStatus = async (taskId, newStatus) => {
    try {
      // Optimistic UI update
      setTasks(prev => prev.map(t => t.id === taskId ? { ...t, status: newStatus } : t));
      
      // We only want to send the required fields to the API, but our API PUT requires title, etc.
      // Fetch the full task first or just send the current state
      const taskToUpdate = tasks.find(t => t.id === taskId);
      await axios.put(`${API_URL}/tasks/${taskId}`, {
        title: taskToUpdate.title,
        description: taskToUpdate.description,
        priority: taskToUpdate.priority,
        status: newStatus,
        dueDate: taskToUpdate.dueDate
      });
    } catch (err) {
      console.error(err);
      alert("Failed to update task status! (Check if you are the assignee or a MANAGER)");
      fetchTasks(); // Revert on failure
    }
  };

  const handleDragStart = (e, taskId) => {
    setDraggingTaskId(taskId);
    e.dataTransfer.setData('text/plain', taskId);
    e.currentTarget.style.opacity = '0.5';
  };

  const handleDragEnd = (e) => {
    e.currentTarget.style.opacity = '1';
    setDraggingTaskId(null);
  };

  const handleDragOver = (e) => {
    e.preventDefault(); // Necessary to allow dropping
  };

  const handleDrop = (e, targetStatus) => {
    e.preventDefault();
    const taskId = e.dataTransfer.getData('text');
    if (taskId && draggingTaskId) {
      const task = tasks.find(t => t.id === taskId);
      if (task && task.status !== targetStatus) {
        updateTaskStatus(taskId, targetStatus);
      }
    }
  };

  if (!token) {
    return (
      <div className="login-container">
        <div className="login-box" style={{ maxWidth: '450px' }}>
          <h2>{isLogin ? 'Log in to MiniJira' : 'Register for MiniJira'}</h2>
          
          {isLogin ? (
            <form onSubmit={handleLogin}>
              <input type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} required />
              <input type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} required />
              <button type="submit">Log in</button>
              <p style={{ marginTop: '15px', fontSize: '14px', textAlign: 'center' }}>
                Don't have an account? <span style={{ color: '#0052cc', cursor: 'pointer', fontWeight: 'bold' }} onClick={() => setIsLogin(false)}>Register here</span>
              </p>
            </form>
          ) : (
            <form onSubmit={handleRegister}>
              <input type="text" placeholder="Full Name" value={name} onChange={e => setName(e.target.value)} required />
              <input type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} required />
              <input type="password" placeholder="Password (min 8 chars)" value={password} onChange={e => setPassword(e.target.value)} required minLength={8} />
              
              <div style={{ marginTop: '10px', marginBottom: '15px' }}>
                <label style={{ fontSize: '12px', color: '#5e6c84', display: 'block', marginBottom: '4px' }}>Organization ID</label>
                <input type="text" value="11111111-1111-1111-1111-111111111111" readOnly disabled style={{ backgroundColor: '#f4f5f7', color: '#5e6c84', cursor: 'not-allowed', marginBottom: '4px' }} />
                <span style={{ fontSize: '11px', color: '#0052cc' }}>*Note: For now we only have one default organization.</span>
              </div>

              <button type="submit">Register</button>
              <p style={{ marginTop: '15px', fontSize: '14px', textAlign: 'center' }}>
                Already have an account? <span style={{ color: '#0052cc', cursor: 'pointer', fontWeight: 'bold' }} onClick={() => setIsLogin(true)}>Log in here</span>
              </p>
            </form>
          )}

          <div style={{ marginTop: '30px', padding: '15px', backgroundColor: '#f4f5f7', borderRadius: '5px', fontSize: '13px', color: '#172b4d' }}>
            <h4 style={{ margin: '0 0 10px 0' }}>Sample Test Accounts</h4>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
              <strong>Admin:</strong> <span>admin@nextwave.com / password123</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
              <strong>Manager:</strong> <span>manager@nextwave.com / password123</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
              <strong>Member:</strong> <span>member@nextwave.com / password123</span>
            </div>
            <hr style={{ border: 'none', borderTop: '1px solid #dfe1e6', margin: '12px 0' }} />
            <p style={{ margin: '0', fontStyle: 'italic', lineHeight: '1.4' }}>
              Note: New registrations are created as <strong>MEMBER</strong> by default. Only a Manager or Admin can promote a Member's role.
            </p>
          </div>
        </div>
      </div>
    );
  }

  const columns = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];

  return (
    <div>
      <nav className="navbar">
        <h1><LayoutDashboard /> MiniJira</h1>
        <button onClick={handleLogout} style={{background: 'none', border: 'none', color: 'white', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px'}}>
          <LogOut size={18} /> Logout
        </button>
      </nav>

      <div className="board-container">
        <div className="board-header">
          <h2>Active Sprint</h2>
        </div>
        
        <div className="columns">
          {columns.map(col => (
            <div 
              key={col} 
              className="column"
              onDragOver={handleDragOver}
              onDrop={(e) => handleDrop(e, col)}
            >
              <div className="column-header">{col.replace('_', ' ')}</div>
              <div className="task-list">
                {tasks.filter(t => t.status === col).map(task => (
                  <div 
                    key={task.id} 
                    className="task-card"
                    draggable
                    onDragStart={(e) => handleDragStart(e, task.id)}
                    onDragEnd={handleDragEnd}
                  >
                    <div className="task-title">{task.title}</div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '10px' }}>
                      <span className={`task-priority ${task.priority}`}>{task.priority}</span>
                      <span style={{ fontSize: '12px', color: '#5e6c84' }}>{new Date(task.dueDate).toLocaleDateString()}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default App;
