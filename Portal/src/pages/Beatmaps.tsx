import { Music, Download, FileText } from 'lucide-react';
import backgroundImg from '../assets/background1.png';

export default function Beatmaps() {
  return (
    <div 
      style={{ 
        width: '100%', 
        minHeight: 'calc(100vh - 64px - 36px)', 
        backgroundImage: `linear-gradient(rgba(21, 17, 27, 0.88), rgba(21, 17, 27, 0.88)), url(${backgroundImg})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundAttachment: 'fixed',
        padding: '64px 24px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center'
      }}
    >
      <div className="container" style={{ maxWidth: '800px', zIndex: 1 }}>
        
        {/* HEADER SECTION */}
        <div style={{ textAlign: 'center', marginBottom: '48px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifySelf: 'center', gap: '12px', marginBottom: '16px' }}>
            <Music className="text-[#00e5ff]" size={32} />
            <h1 style={{ fontSize: '2.2rem', fontWeight: 800, letterSpacing: '-0.02em', color: '#fff', margin: 0 }}>
              Song Packs
            </h1>
          </div>
        </div>

        {/* SONG PACKS HUB CARD */}
        <div 
          className="glass-panel" 
          style={{ 
            padding: '40px', 
            borderRadius: '4px', 
            backgroundColor: 'var(--bg-card)', 
            border: '1px solid var(--border-color)',
            boxShadow: '0 12px 40px rgba(0, 0, 0, 0.35)',
            display: 'flex',
            flexDirection: 'column',
            gap: '24px',
            position: 'relative',
            overflow: 'hidden'
          }}
        >
          {/* Subtle Accent Edge */}
          <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '3px', background: 'linear-gradient(90deg, var(--color-pink), var(--color-purple))' }} />

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
            <div>
              <span 
                style={{ 
                  fontSize: '0.7rem', 
                  textTransform: 'uppercase', 
                  fontWeight: 800, 
                  letterSpacing: '0.15em', 
                  color: 'var(--color-pink)',
                  display: 'inline-block',
                  marginBottom: '8px'
                }}
              >
                Official Release Pack
              </span>
              <h2 style={{ fontSize: '1.8rem', fontWeight: 800, color: '#fff', margin: 0 }}>
                songs-pack-1
              </h2>
            </div>
            
            <div 
              style={{ 
                display: 'flex', 
                alignItems: 'center', 
                gap: '6px', 
                backgroundColor: 'rgba(255, 255, 255, 0.03)', 
                padding: '6px 12px', 
                borderRadius: '4px',
                border: '1px solid rgba(255, 255, 255, 0.05)',
                fontSize: '0.75rem',
                color: 'var(--color-text-secondary)'
              }}
            >
              <FileText size={12} />
              Archive (.zip)
            </div>
          </div>



          <div 
            style={{ 
              display: 'flex', 
              justifyContent: 'flex-start', 
              marginTop: '8px' 
            }}
          >
            <a 
              href="https://drive.google.com/file/d/1hXMg4sbnyfvrfci371gQcytrQ9iSPLh8/view?usp=sharing" 
              target="_blank" 
              rel="noopener noreferrer"
              className="glow-btn-cyan"
              style={{ 
                display: 'inline-flex',
                alignItems: 'center',
                gap: '10px',
                textDecoration: 'none',
                padding: '14px 28px',
                fontSize: '0.95rem',
                fontWeight: 700,
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              <Download size={16} />
              Download from Google Drive
            </a>
          </div>

        </div>

      </div>
    </div>
  );
}
