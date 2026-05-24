import { useEffect, useState } from 'react';
import { Search, Trophy, Loader2 } from 'lucide-react';
import { API_BASE_URL } from '../config';

interface UserLeaderboardEntry {
  username: string;
  profilePictureUrl: string;
  volforce: number;
}

interface LeaderboardProps {
  onSelectUser: (username: string) => void;
}

export default function Leaderboard({ onSelectUser }: LeaderboardProps) {
  const [users, setUsers] = useState<UserLeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterQuery, setFilterQuery] = useState('');

  useEffect(() => {
    const fetchLeaderboard = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/users/leaderboard`);
        if (res.ok) {
          const data = await res.json();
          const sorted = data.sort((a: any, b: any) => b.volforce - a.volforce);
          setUsers(sorted);
        } else {
          setError('Failed to fetch player leaderboard rankings.');
        }
      } catch (e) {
        setError('Could not connect to the backend server. Please verify it is running.');
      } finally {
        setLoading(false);
      }
    };
    fetchLeaderboard();
  }, []);

  const filteredUsers = users.filter(user => 
    user.username.toLowerCase().includes(filterQuery.toLowerCase())
  );

  return (
    <div className="container" style={{ paddingBottom: '80px' }}>
      
      {/* HEADER SECTION */}
      <div className="leaderboard-header">
        <div>
          <h1 className="leaderboard-title">
            <Trophy className="text-[#ffb300]" size={28} />
            Global Volforce Rankings
          </h1>
          <p className="leaderboard-subtitle">
            Top simulator players ranked by their overall best 10 charts performance.
          </p>
        </div>

        {/* INLINE RANKING SEARCH */}
        <div className="filter-wrapper">
          <input
            type="text"
            placeholder="Filter by player username..."
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
          <span style={{ fontSize: '0.75rem' }}>Loading player rankings...</span>
        </div>
      )}

      {!loading && error && (
        <div className="glass-panel" style={{ padding: '32px', textAlign: 'center', color: 'var(--color-pink)', fontSize: '0.8rem', border: '1px solid rgba(255, 0, 127, 0.15)' }}>
          {error}
          <p style={{ fontSize: '0.65rem', color: '#5d5a75', marginTop: '8px' }}>
            Tip: Run your Spring Boot server locally (port 8082) or deploy to NeonDB.
          </p>
        </div>
      )}

      {!loading && !error && (
        <div className="table-container glass-panel">
          <table className="leaderboard-table">
            <thead>
              <tr>
                <th className="rank-col">Rank</th>
                <th>Player</th>
                <th style={{ textAlign: 'right', paddingRight: '24px' }}>Rating</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={3} style={{ padding: '48px', textAlign: 'center', color: '#5d5a75', fontStyle: 'italic' }}>
                    No matching players found.
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user, index) => {
                  const rank = index + 1;
                  const isTop3 = rank <= 3;
                  const rankColors = ['#ffb300', '#d3d3d3', '#cd7f32'];
                  
                  return (
                    <tr 
                      key={index} 
                      className="animate-fadeIn"
                      style={{ animationDelay: `${index * 0.02}s` }}
                    >
                      {/* RANK */}
                      <td className="rank-col">
                        <span style={{ color: isTop3 ? rankColors[rank - 1] : '#5d5a75' }}>
                          #{rank}
                        </span>
                      </td>

                      {/* PLAYER PROFILE */}
                      <td>
                        <div className="player-col">
                          {/* Avatar */}
                          <div className="avatar-wrapper">
                            {user.profilePictureUrl ? (
                              <img 
                                src={user.profilePictureUrl} 
                                alt="" 
                                className="avatar-img"
                                onError={(e) => {
                                  (e.target as HTMLElement).style.display = 'none';
                                }}
                              />
                            ) : null}
                            <span className="avatar-fallback">
                              {user.username.substring(0, 2)}
                            </span>
                          </div>
                          
                          {/* Username Link */}
                          <button
                            onClick={() => onSelectUser(user.username)}
                            className="username-btn"
                          >
                            {user.username}
                          </button>
                        </div>
                      </td>

                      {/* VOLFORCE (VF) */}
                      <td className="rating-col" style={{ paddingRight: '24px' }}>
                        {user.volforce.toFixed(3)} <span className="rating-unit">VF</span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      )}

    </div>
  );
}
