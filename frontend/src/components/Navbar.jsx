import React from 'react';
import { LayoutDashboard, LogOut, ArrowLeft } from 'lucide-react';
import { useNavigate, useLocation } from 'react-router-dom';

function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.location.href = '/login';
  };

  const isDashboard = location.pathname === '/';

  return (
    <nav className="navbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
        {!isDashboard && (
          <button 
            onClick={() => navigate('/')} 
            style={{background: 'none', border: 'none', color: 'white', cursor: 'pointer', display: 'flex', alignItems: 'center'}}
          >
            <ArrowLeft size={24} />
          </button>
        )}
        <h1 style={{ cursor: 'pointer' }} onClick={() => navigate('/')}>
          <LayoutDashboard style={{ marginRight: '8px' }} /> MiniJira
        </h1>
      </div>
      <button onClick={handleLogout} style={{background: 'none', border: 'none', color: 'white', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px'}}>
        <LogOut size={18} /> Logout
      </button>
    </nav>
  );
}

export default Navbar;
