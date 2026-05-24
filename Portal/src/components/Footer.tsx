import { API_BASE_URL } from '../config';

interface FooterProps {
  setCurrentTab: (tab: string) => void;
}

export default function Footer({ setCurrentTab }: FooterProps) {
  return (
    <footer className="footer">
      <div className="footer-container">
        
        {/* LOGO & DESCRIPTION */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontFamily: 'var(--font-heading)', fontWeight: 800, color: 'var(--color-cyan)' }}>⚡ NodeVoltex</span>
            <span style={{ fontSize: '0.65rem', color: '#5d5a75' }}>v1.0.0</span>
          </div>
          <p className="footer-desc">
            The next-generation Sound Voltex simulator tracking database and competitive portal.
          </p>
        </div>

        {/* QUICK LINKS */}
        <div className="footer-links">
          <button 
            onClick={() => setCurrentTab('home')} 
            className="footer-link"
          >
            Home
          </button>
          <button 
            onClick={() => setCurrentTab('leaderboard')} 
            className="footer-link"
          >
            Leaderboard
          </button>
          <button 
            onClick={() => setCurrentTab('beatmaps')} 
            className="footer-link"
          >
            Beatmaps
          </button>
          <a 
            href={`${API_BASE_URL}/swagger-ui/index.html`}
            target="_blank" 
            rel="noopener noreferrer"
            className="footer-link"
            style={{ color: 'var(--color-pink)' }}
          >
            Backend API
          </a>
        </div>

        {/* COPYRIGHT */}
        <div className="footer-copy">
          &copy; {new Date().getFullYear()} NodeVoltex Team. Built for S4 Netlab Game Project.
        </div>

      </div>
    </footer>
  );
}
