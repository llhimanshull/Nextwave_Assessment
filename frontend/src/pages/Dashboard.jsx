import React from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { Users, FolderKanban } from 'lucide-react';

function Dashboard() {
  const navigate = useNavigate();
  const userStr = localStorage.getItem('user');
  const user = userStr ? JSON.parse(userStr) : null;
  const role = user?.role || 'MEMBER';

  const canManage = role === 'ADMIN' || role === 'MANAGER';
  const isAdmin = role === 'ADMIN';

  return (
    <div>
      <Navbar />
      <div className="page-container">
        <h2 className="page-title">Dashboard</h2>

        <div style={{ marginBottom: '12px', fontSize: '0.9rem', color: '#5e6c84' }}>
          Welcome, <strong>{user?.name || 'User'}</strong> — Role: <span className={`badge badge-${role.toLowerCase()}`}>{role}</span>
          {' | '}Organization: <code style={{ fontSize: '0.75rem' }}>{user?.organizationId}</code>
        </div>

        <div className="dashboard-grid" style={{ marginTop: '24px' }}>
          <div className="dash-card" onClick={() => navigate('/profile')}>
            <h3><Users size={20} /> My Profile</h3>
            <p>View and update your personal details.</p>
          </div>

          {isAdmin && (
            <div className="dash-card" onClick={() => navigate('/users')}>
              <h3><Users size={20} /> Manage Users</h3>
              <p>View all users in your organization and update their roles and details.</p>
            </div>
          )}

          <div className="dash-card" onClick={() => navigate('/projects')}>
            <h3><FolderKanban size={20} /> {canManage ? 'Manage Projects' : 'View Projects'}</h3>
            <p>{canManage ? 'Create, edit, or select a project to manage its tasks.' : 'View projects and your assigned tasks.'}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
