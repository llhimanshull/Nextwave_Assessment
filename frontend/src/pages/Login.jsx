import React, { useState } from 'react';
import axios from 'axios';

const API = 'http://localhost:8080/api/v1';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [isLogin, setIsLogin] = useState(true);
  const [msg, setMsg] = useState({ text: '', type: '' });

  const handleLogin = async (e) => {
    e.preventDefault();
    setMsg({ text: '', type: '' });
    try {
      const res = await axios.post(`${API}/auth/login`, { email, password });
      localStorage.setItem('token', res.data.accessToken);
      localStorage.setItem('refreshToken', res.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(res.data.user));
      window.location.href = '/';
    } catch (err) {
      setMsg({ text: 'Login failed! Please check your credentials.', type: 'error' });
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setMsg({ text: '', type: '' });
    try {
      await axios.post(`${API}/auth/register`, {
        name, email, password,
        organizationId: '11111111-1111-1111-1111-111111111111'
      });
      setMsg({ text: 'Registration successful! You can now log in.', type: 'success' });
      setIsLogin(true);
    } catch (err) {
      const detail = err.response?.data?.message || 'Registration failed. Email might already be in use.';
      setMsg({ text: detail, type: 'error' });
    }
  };

  return (
    <div className="login-container">
      <div className="login-box" style={{ maxWidth: '450px' }}>
        <h2>{isLogin ? 'Log in to MiniJira' : 'Register for MiniJira'}</h2>

        {msg.text && <div className={msg.type === 'success' ? 'msg-success' : 'msg-error'}>{msg.text}</div>}

        {isLogin ? (
          <form onSubmit={handleLogin}>
            <input type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} required />
            <input type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} required />
            <button type="submit">Log in</button>
            <p style={{ marginTop: '15px', fontSize: '14px', textAlign: 'center' }}>
              Don't have an account? <span style={{ color: '#0052cc', cursor: 'pointer', fontWeight: 'bold' }} onClick={() => { setIsLogin(false); setMsg({ text: '', type: '' }); }}>Register here</span>
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
              <span style={{ fontSize: '11px', color: '#0052cc' }}>*For now we only have one default organization.</span>
            </div>

            <button type="submit">Register</button>
            <p style={{ marginTop: '15px', fontSize: '14px', textAlign: 'center' }}>
              Already have an account? <span style={{ color: '#0052cc', cursor: 'pointer', fontWeight: 'bold' }} onClick={() => { setIsLogin(true); setMsg({ text: '', type: '' }); }}>Log in here</span>
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
            New registrations are created as <strong>MEMBER</strong> by default. Only a Manager or Admin can promote a Member's role.
          </p>
        </div>
      </div>
    </div>
  );
}

export default Login;
