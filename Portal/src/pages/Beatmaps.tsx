import { useEffect, useState } from 'react';
import { Music, Search, Loader2 } from 'lucide-react';
import { API_BASE_URL } from '../config';

interface BeatmapEntry {
  id: string;
  title: string;
  artist: string;
  difficulty: string;
  level: number;
}

export default function Beatmaps() {
  const [beatmaps, setBeatmaps] = useState<BeatmapEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterQuery, setFilterQuery] = useState('');

  useEffect(() => {
    const fetchBeatmaps = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/beatmaps`);
        if (res.ok) {
          const data = await res.json();
          setBeatmaps(data);
        } else {
          setError('Failed to fetch beatmaps list.');
        }
      } catch (e) {
        setError('Could not connect to the backend server. Please verify it is running.');
      } finally {
        setLoading(false);
      }
    };
    fetchBeatmaps();
  }, []);

  const filteredMaps = beatmaps.filter(map => 
    map.title.toLowerCase().includes(filterQuery.toLowerCase()) ||
    map.artist.toLowerCase().includes(filterQuery.toLowerCase()) ||
    map.difficulty.toLowerCase().includes(filterQuery.toLowerCase())
  );

  const getDifficultyColor = (diff: string) => {
    switch (diff.toUpperCase()) {
      case 'EXHAUST': return 'text-[#ff007f]';
      case 'MAXIMUM': return 'text-[#8c6df0]';
      case 'GRAVITY': return 'text-[#ffb300]';
      case 'ADVANCED': return 'text-[#ffeb3b]';
      case 'NOVICE': return 'text-[#00e5ff]';
      default: return 'text-white';
    }
  };

  return (
    <div className="container" style={{ paddingBottom: '80px', paddingTop: '48px' }}>
      
      {/* HEADER SECTION */}
      <div className="leaderboard-header">
        <div>
          <h1 className="leaderboard-title">
            <Music className="text-[#00e5ff]" size={28} />
            Parsed Song Beatmaps
          </h1>
          <p className="leaderboard-subtitle">
            Browse and discover loaded .ksh sound files interpreted by the NodeVoltex server.
          </p>
        </div>

        {/* INLINE SEARCH */}
        <div className="filter-wrapper">
          <input
            type="text"
            placeholder="Search title, artist, mapper..."
            value={filterQuery}
            onChange={(e) => setFilterQuery(e.target.value)}
            className="filter-input"
          />
          <Search size={14} style={{ position: 'absolute', right: '14px', top: '12px', color: '#5d5a75' }} />
        </div>
      </div>

      {/* STATE PANELS */}
      {loading && (
        <div style={{ padding: '80px 0', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px', color: '#a09eb5' }}>
          <Loader2 className="animate-spin text-[#00e5ff]" size={32} />
          <span style={{ fontSize: '0.75rem' }}>Loading chart databases...</span>
        </div>
      )}

      {!loading && error && (
        <div className="glass-panel" style={{ padding: '32px', textAlign: 'center', color: 'var(--color-pink)', fontSize: '0.8rem', border: '1px solid rgba(255, 0, 127, 0.15)' }}>
          {error}
          <p style={{ fontSize: '0.65rem', color: '#5d5a75', marginTop: '8px' }}>
            Tip: Standard maps are initialized in your server when you start plays or upload scores.
          </p>
        </div>
      )}

      {!loading && !error && (
        <div className="beatmaps-grid">
          {filteredMaps.length === 0 ? (
            <div className="glass-panel" style={{ gridColumn: '1 / -1', padding: '48px', fontStyle: 'italic', color: '#5d5a75', textAlign: 'center', fontSize: '0.8rem' }}>
              No matching beatmaps found in the database.
            </div>
          ) : (
            filteredMaps.map((map, index) => (
              <div 
                key={index} 
                className="beatmap-card glass-panel"
              >
                <div className="beatmap-card-header">
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <h3 className="beatmap-card-title truncate" title={map.title}>
                      {map.title}
                    </h3>
                    <p className="beatmap-card-artist truncate" title={map.artist}>
                      {map.artist}
                    </p>
                  </div>
                  
                  {/* Level Badge */}
                  <div className="beatmap-card-level">
                    {map.level}
                  </div>
                </div>

                <div className="beatmap-card-footer">
                  <span className={`beatmap-card-diff ${getDifficultyColor(map.difficulty)}`}>
                    {map.difficulty}
                  </span>
                  
                  <span className="beatmap-card-id">
                    ID: {map.id.substring(0, Math.min(map.id.length, 12))}...
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      )}

    </div>
  );
}
