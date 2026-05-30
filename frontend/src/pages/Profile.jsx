import React, { useState, useEffect } from 'react';
import axios from 'axios';
import Navbar from '../components/Navbar';

const API = 'http://localhost:8080/api/v1';

function Profile() {
  const [msg, setMsg] = useState({ text: '', type: '' });
  const [profileForm, setProfileForm] = useState({ name: '', email: '' });

  const userStr = localStorage.getItem('user');
  const me = userStr ? JSON.parse(userStr) : null;

  useEffect(() => {
    if (me) {
      setProfileForm({ name: me.name || '', email: me.email || '' });
    }
  }, []);

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.put(`${API}/users/${me.id}`, {
        name: profileForm.name,
        email: profileForm.email
      });

      // Update local storage with new info
      const updatedUser = { ...me, name: res.data.name, email: res.data.email };
      localStorage.setItem('user', JSON.stringify(updatedUser));

      setMsg({ text: `Profile updated successfully!`, type: 'success' });
    } catch (err) {
      const detail = err.response?.data?.message || 'Failed to update profile.';
      setMsg({ text: detail, type: 'error' });
    }
  };

  if (!me) return <div>Loading...</div>;

  return (
    <div>
      <Navbar />
      <div className="page-container">
        <div className="page-header">
          <h2>My Profile</h2>
        </div>

        {msg.text && <div className={msg.type === 'success' ? 'msg-success' : 'msg-error'}>{msg.text}</div>}

        <div className="form-card" style={{ maxWidth: '500px' }}>
          <form onSubmit={handleUpdate}>
            <div className="form-group">
              <label>Name</label>
              <input 
                type="text" 
                value={profileForm.name} 
                onChange={e => setProfileForm({...profileForm, name: e.target.value})} 
                required 
              />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input 
                type="email" 
                value={profileForm.email} 
                onChange={e => setProfileForm({...profileForm, email: e.target.value})} 
                required 
              />
            </div>
            <div className="form-group">
              <label>Role</label>
              <input type="text" value={me.role} disabled />
              <p className="form-info">Your role is managed by your organization Administrator.</p>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary">Save Profile</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default Profile;
