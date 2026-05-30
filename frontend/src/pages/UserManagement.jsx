import React, { useState, useEffect } from 'react';
import axios from 'axios';
import Navbar from '../components/Navbar';

const API = 'http://localhost:8080/api/v1';

function UserManagement() {
  const [users, setUsers] = useState([]);
  const [editingUser, setEditingUser] = useState(null);
  const [editForm, setEditForm] = useState({ name: '', email: '', role: '' });
  const [msg, setMsg] = useState({ text: '', type: '' });

  const userStr = localStorage.getItem('user');
  const me = userStr ? JSON.parse(userStr) : null;

  // If somehow a non-admin gets here, the API will reject it anyway
  // But we only show the link for ADMIN in Dashboard.

  useEffect(() => { fetchUsers(); }, []);

  const fetchUsers = async () => {
    try {
      const res = await axios.get(`${API}/users`);
      setUsers(res.data);
    } catch (err) {
      setMsg({ text: 'Failed to load users. You may not have permission.', type: 'error' });
    }
  };

  const openEdit = (u) => {
    setEditingUser(u);
    setEditForm({ name: u.name, email: u.email, role: u.role });
    setMsg({ text: '', type: '' });
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      // 1. Update Details
      await axios.put(`${API}/users/${editingUser.id}`, {
        name: editForm.name,
        email: editForm.email
      });

      // 2. Update Role if it changed
      if (editForm.role !== editingUser.role) {
        await axios.put(`${API}/users/${editingUser.id}/role`, { role: editForm.role });
      }

      setMsg({ text: `User updated successfully!`, type: 'success' });
      setEditingUser(null);
      fetchUsers();
    } catch (err) {
      const detail = err.response?.data?.message || 'Failed to update user.';
      setMsg({ text: detail, type: 'error' });
    }
  };

  return (
    <div>
      <Navbar />
      <div className="page-container">
        <div className="page-header">
          <h2>User Management (Admin Only)</h2>
        </div>

        {msg.text && <div className={msg.type === 'success' ? 'msg-success' : 'msg-error'}>{msg.text}</div>}

        {/* Edit User Form */}
        {editingUser && (
          <div className="form-card" style={{ marginBottom: '24px' }}>
            <h3>Update User — {editingUser.name}</h3>
            <form onSubmit={handleUpdate}>
              <div className="form-group">
                <label>Name</label>
                <input type="text" value={editForm.name} onChange={e => setEditForm({...editForm, name: e.target.value})} required />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={editForm.email} onChange={e => setEditForm({...editForm, email: e.target.value})} required />
              </div>
              <div className="form-group">
                <label>Role</label>
                <select value={editForm.role} onChange={e => setEditForm({...editForm, role: e.target.value})}>
                  {['MEMBER', 'MANAGER', 'ADMIN'].map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary">Save Changes</button>
                <button type="button" className="btn btn-secondary" onClick={() => setEditingUser(null)}>Cancel</button>
              </div>
            </form>
          </div>
        )}

        {/* Users Table */}
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Organization</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id}>
                <td>{u.name}</td>
                <td>{u.email}</td>
                <td><span className={`badge badge-${u.role.toLowerCase()}`}>{u.role}</span></td>
                <td><code style={{ fontSize: '0.7rem' }}>{u.organizationId}</code></td>
                <td>
                  {u.id !== me?.id ? (
                    <button className="btn btn-outline btn-sm" onClick={() => openEdit(u)}>Edit</button>
                  ) : (
                    <span style={{ color: '#5e6c84', fontSize: '0.8rem' }}>You (Edit in Profile)</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default UserManagement;
