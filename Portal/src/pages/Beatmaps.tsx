import { Music, Download } from 'lucide-react';
import backgroundImg from '../assets/background1.png';

interface SongPack {
  id: string;
  name: string;
  songsCount: number;
  size: string;
  link: string;
  badge: string;
}

const songPacks: SongPack[] = [
  {
    id: 'pack-1',
    name: 'songs-pack-1',
    songsCount: 54,
    size: '272 MB',
    link: 'https://drive.google.com/file/d/1hXMg4sbnyfvrfci371gQcytrQ9iSPLh8/view?usp=sharing',
    badge: 'Official Release Pack'
  },
  {
    id: 'pack-2',
    name: 'songs-pack-2',
    songsCount: 29,
    size: '156 MB',
    link: 'https://drive.google.com/file/d/1ZtOwHde4m59B5wnYiqwkCdQiFuZcBf7b/view?usp=sharing',
    badge: 'Expansion Pack A'
  },
  {
    id: 'pack-3',
    name: 'songs-pack-3',
    songsCount: 35,
    size: '171 MB',
    link: 'https://drive.google.com/file/d/1qCUMw7hQSX93TdM9wBd87JDL1RNqtDAQ/view?usp=sharing',
    badge: 'Expansion Pack B'
  },
  {
    id: 'pack-4',
    name: 'songs-pack-4',
    songsCount: 47,
    size: '200 MB',
    link: 'https://drive.google.com/file/d/1eIsIGU4spIMrBPxxGJKXXeT5h_NdS7MV/view?usp=sharing',
    badge: 'Expansion Pack C'
  }
];

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
      <div className="container" style={{ maxWidth: '1000px', zIndex: 1 }}>
        
        {/* HEADER SECTION */}
        <div style={{ textAlign: 'center', marginBottom: '48px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', justifySelf: 'center', gap: '12px', marginBottom: '16px' }}>
            <Music className="text-[#00e5ff]" size={32} />
            <h1 style={{ fontSize: '2.2rem', fontWeight: 800, letterSpacing: '-0.02em', color: '#fff', margin: 0 }}>
              Song Packs
            </h1>
          </div>
        </div>

        {/* SONG PACKS SCROLLABLE GRID */}
        <div 
          style={{ 
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: '24px',
            width: '100%',
            marginBottom: '40px'
          }}
        >
          {songPacks.map((pack) => (
            <div 
              key={pack.id}
              className="glass-panel" 
              style={{ 
                padding: '30px', 
                borderRadius: '4px', 
                backgroundColor: 'var(--bg-card)', 
                border: '1px solid var(--border-color)',
                boxShadow: '0 12px 40px rgba(0, 0, 0, 0.35)',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
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
                    {pack.badge}
                  </span>
                  <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: '#fff', margin: 0 }}>
                    {pack.name}
                  </h2>
                </div>
                
                <div 
                  style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    gap: '6px', 
                    backgroundColor: 'rgba(0, 229, 255, 0.05)', 
                    padding: '6px 12px', 
                    borderRadius: '4px',
                    border: '1px solid rgba(0, 229, 255, 0.15)',
                    fontSize: '0.72rem',
                    color: '#00e5ff',
                    fontWeight: 600
                  }}
                >
                  <Music size={12} />
                  {pack.songsCount} Songs, {pack.size}
                </div>
              </div>

              <div 
                style={{ 
                  display: 'flex', 
                  justifyContent: 'stretch', 
                  marginTop: '8px' 
                }}
              >
                <a 
                  href={pack.link} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="glow-btn-cyan"
                  style={{ 
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '10px',
                    textDecoration: 'none',
                    padding: '12px 24px',
                    fontSize: '0.9rem',
                    fontWeight: 700,
                    borderRadius: '4px',
                    cursor: 'pointer',
                    width: '100%',
                    boxSizing: 'border-box'
                  }}
                >
                  <Download size={16} />
                  Download Pack
                </a>
              </div>

            </div>
          ))}
        </div>

      </div>
    </div>
  );
}
