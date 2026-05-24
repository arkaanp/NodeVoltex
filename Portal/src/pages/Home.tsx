import { Download, Music } from 'lucide-react';
import backgroundImg from '../assets/background1.png';
import titleImg from '../assets/title.png';

interface HomeProps {
  setCurrentTab: (tab: string) => void;
}

export default function Home({ setCurrentTab }: HomeProps) {
  return (
    <div 
      style={{ 
        width: '100%', 
        minHeight: 'calc(100vh - 64px - 36px)', /* subtracting navbar & footer heights */
        display: 'flex', 
        flexDirection: 'column', 
        alignItems: 'center', 
        justifyContent: 'center',
        backgroundImage: `linear-gradient(rgba(21, 17, 27, 0.05), rgba(21, 17, 27, 0.05)), url(${backgroundImg})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundAttachment: 'fixed',
        padding: '48px 24px',
        position: 'relative'
      }}
    >
      <div 
        style={{ 
          display: 'flex', 
          flexDirection: 'column', 
          alignItems: 'center', 
          gap: '32px',
          maxWidth: '600px',
          width: '100%',
          textAlign: 'center',
          zIndex: 1
        }}
      >
        {/* CENTERED TITLE IMAGE */}
        <img 
          src={titleImg} 
          alt="NodeVoltex" 
          style={{ 
            width: '100%', 
            maxWidth: '460px', 
            height: 'auto', 
            objectFit: 'contain',
            filter: 'drop-shadow(0 8px 24px rgba(0, 0, 0, 0.45))'
          }} 
        />

        {/* BUTTONS GROUP */}
        <div 
          style={{ 
            display: 'flex', 
            gap: '16px', 
            width: '100%', 
            justifyContent: 'center',
            flexWrap: 'wrap'
          }}
        >
          <a 
            href="https://github.com/arkaanp/NodeVoltex/releases" 
            target="_blank" 
            rel="noopener noreferrer"
            className="glow-btn-cyan"
            style={{ 
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              textDecoration: 'none',
              padding: '12px 24px',
              fontSize: '0.9rem',
              fontWeight: 600,
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            <Download size={16} />
            Download Client
          </a>

          <button 
            onClick={() => setCurrentTab('beatmaps')}
            className="glow-btn-pink"
            style={{ 
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '12px 24px',
              fontSize: '0.9rem',
              fontWeight: 600,
              borderRadius: '4px',
              border: 'none',
              cursor: 'pointer'
            }}
          >
            <Music size={16} />
            Download Beatmaps
          </button>
        </div>
      </div>
    </div>
  );
}
