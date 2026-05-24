import { useState } from 'react';
import { Search, Trophy, Music, Home, Menu, X } from 'lucide-react';

interface NavbarProps {
  currentTab: string;
  setCurrentTab: (tab: string) => void;
  onSearchUser: (username: string) => void;
}

export default function Navbar({ currentTab, setCurrentTab, onSearchUser }: NavbarProps) {
  const [searchVal, setSearchVal] = useState('');
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchVal.trim()) {
      onSearchUser(searchVal.trim());
      setSearchVal('');
      setMobileOpen(false);
    }
  };

  const navItems = [
    { id: 'home', label: 'Home', icon: Home },
    { id: 'leaderboard', label: 'Leaderboard', icon: Trophy },
    { id: 'beatmaps', label: 'Beatmaps', icon: Music },
  ];

  return (
    <nav className="navbar">
      <div className="navbar-container">
        
        {/* LOGO */}
        <div 
          onClick={() => setCurrentTab('home')} 
          className="logo-link"
        >
          <div className="logo-icon">
            ⚡
          </div>
          <span className="logo-text">
            NodeVoltex <span>HUB</span>
          </span>
        </div>

        {/* DESKTOP NAV */}
        <div className="nav-links">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setCurrentTab(item.id)}
                className={`nav-item ${isActive ? 'nav-item-active' : ''}`}
              >
                <Icon size={14} />
                {item.label}
              </button>
            );
          })}
        </div>

        {/* SEARCH BAR */}
        <form 
          onSubmit={handleSearchSubmit} 
          className="search-wrapper"
        >
          <input
            type="text"
            placeholder="Search player username..."
            value={searchVal}
            onChange={(e) => setSearchVal(e.target.value)}
            className="search-field"
          />
          <button 
            type="submit" 
            className="search-icon-btn"
          >
            <Search size={14} />
          </button>
        </form>

        {/* MOBILE TOGGLE */}
        <button 
          onClick={() => setMobileOpen(!mobileOpen)} 
          className="mobile-menu-btn"
        >
          {mobileOpen ? <X size={20} /> : <Menu size={20} />}
        </button>

      </div>

      {/* MOBILE NAV PANELS */}
      {mobileOpen && (
        <div className="mobile-menu-panel" style={{ display: 'flex' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = currentTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setCurrentTab(item.id);
                    setMobileOpen(false);
                  }}
                  className={`nav-item ${isActive ? 'nav-item-active' : ''}`}
                  style={{ width: '100%', justifyContent: 'flex-start', padding: '12px' }}
                >
                  <Icon size={16} />
                  {item.label}
                </button>
              );
            })}
          </div>

          <form onSubmit={handleSearchSubmit} className="search-wrapper" style={{ width: '100%' }}>
            <input
              type="text"
              placeholder="Search player username..."
              value={searchVal}
              onChange={(e) => setSearchVal(e.target.value)}
              className="search-field"
              style={{ borderRadius: '8px' }}
            />
            <button type="submit" className="search-icon-btn" style={{ right: '12px' }}>
              <Search size={16} />
            </button>
          </form>
        </div>
      )}
    </nav>
  );
}
