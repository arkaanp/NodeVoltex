import { useEffect, useState } from 'react';
import { Award, Activity, Loader2, User } from 'lucide-react';
import { API_BASE_URL } from '../config';

interface ScoreEntry {
  mapId: string;
  title: string;
  artist: string;
  difficulty: string;
  level: number;
  score: number;
  grade: string;
  volforce: number;
  maxCombo: number;
  sCriticals: number;
  criticals: number;
  nears: number;
  mids: number;
  fars: number;
  misses: number;
  laserTicks: number;
  laserMisses: number;
  early: number;
  late: number;
  timestamp: number;
}

interface ProfileProps {
  username: string;
}

export default function Profile({ username }: ProfileProps) {
  const [profile, setProfile] = useState<any>(null);
  const [bestScores, setBestScores] = useState<ScoreEntry[]>([]);
  const [recentScores, setRecentScores] = useState<ScoreEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState<'best' | 'recent'>('best');

  useEffect(() => {
    const fetchProfileData = async () => {
      setLoading(true);
      setError('');
      try {
        const profRes = await fetch(`${API_BASE_URL}/api/users/profile/${username}`);
        if (!profRes.ok) {
          setError('Player not found.');
          setLoading(false);
          return;
        }
        const profData = await profRes.json();
        
        let globalRank = '-';
        try {
          const leaderRes = await fetch(`${API_BASE_URL}/api/users/leaderboard`);
          if (leaderRes.ok) {
            const leaders = await leaderRes.json();
            const idx = leaders.findIndex((u: any) => u.username.toLowerCase() === username.toLowerCase());
            if (idx !== -1) globalRank = `#${idx + 1}`;
          }
        } catch (e) {}
        
        setProfile({ ...profData, globalRank });

        const bestRes = await fetch(`${API_BASE_URL}/api/scores/user/${username}/best`);
        if (bestRes.ok) {
          const bestData = await bestRes.json();
          setBestScores(bestData);
        }

        const recentRes = await fetch(`${API_BASE_URL}/api/scores/user/${username}/recent`);
        if (recentRes.ok) {
          const recentData = await recentRes.json();
          setRecentScores(recentData);
        }

      } catch (e) {
        setError('Error loading profile. Verify your backend server is online.');
      } finally {
        setLoading(false);
      }
    };
    fetchProfileData();
  }, [username]);

  const getClearBadge = (score: ScoreEntry) => {
    const miss = score.misses || 0;
    const lMiss = score.laserMisses || 0;
    const near = score.nears || 0;
    const far = score.fars || 0;

    if (miss === 0 && lMiss === 0 && near === 0 && far === 0) {
      return { text: 'PUC', className: 'clear-puc' };
    } else if (miss === 0 && lMiss === 0) {
      return { text: 'UC', className: 'clear-uc' };
    } else {
      return { text: 'Normal', className: 'clear-normal' };
    }
  };

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

  const formatTimestamp = (ts: number) => {
    const date = new Date(ts);
    return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' }) + ' ' + 
           date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="container profile-wrapper" style={{ paddingBottom: '80px' }}>
      
      {loading && (
        <div style={{ padding: '80px 0', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px', color: '#a09eb5' }}>
          <Loader2 className="animate-spin text-[#00e5ff]" size={32} />
          <span style={{ fontSize: '0.75rem' }}>Loading player profile details...</span>
        </div>
      )}

      {!loading && error && (
        <div className="glass-panel" style={{ padding: '32px', textAlign: 'center', color: 'var(--color-pink)', fontSize: '0.8rem', border: '1px solid rgba(255, 0, 127, 0.15)' }}>
          {error}
        </div>
      )}

      {!loading && profile && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '32px', width: '100%' }}>
          
          {/* PROFILE SUMMARY HEADER */}
          <div className="profile-header glass-panel">
            <div className="hero-glow-1" style={{ top: 0, right: 0, left: 'auto', bottom: 'auto', width: '200px', height: '200px' }} />
            
            {/* Avatar */}
            <div className="profile-avatar">
              {profile.profilePictureUrl ? (
                <img 
                  src={profile.profilePictureUrl} 
                  alt="" 
                  className="avatar-img"
                  onError={(e) => {
                    (e.target as HTMLElement).style.display = 'none';
                  }}
                />
              ) : null}
              <User size={40} className="text-[#5d5a75]" />
            </div>

            {/* Details */}
            <div className="profile-details">
              <h1 className="profile-name">
                {profile.username}
              </h1>
              
              <div className="profile-meta-row">
                <div className="profile-meta-item">
                  <Award size={14} className="text-[#ffb300]" />
                  Global Rank: <span style={{ fontWeight: 700, color: '#fff' }}>{profile.globalRank}</span>
                </div>
                <div className="profile-meta-item">
                  <Activity size={14} className="text-[#ff007f]" />
                  Plays: <span style={{ fontWeight: 700, color: '#fff' }}>{recentScores.length}</span>
                </div>
              </div>
            </div>

            {/* Volforce Score Widget */}
            <div className="profile-rating-box glass-panel">
              <span className="profile-rating-title">
                Overall Rating
              </span>
              <span className="profile-rating-value">
                {profile.volforce ? profile.volforce.toFixed(3) : '0.000'}
              </span>
              <span className="profile-rating-label">
                Volforce
              </span>
            </div>

          </div>

          {/* TABS CONTROLLER */}
          <div className="tab-bar">
            <button
              onClick={() => setActiveTab('best')}
              className={`tab-btn ${activeTab === 'best' ? 'tab-btn-active' : ''}`}
            >
              Best Plays ({bestScores.length})
            </button>
            <button
              onClick={() => setActiveTab('recent')}
              className={`tab-btn ${activeTab === 'recent' ? 'tab-btn-active-recent' : ''}`}
            >
              Recent Plays ({recentScores.length})
            </button>
          </div>

          {/* PLAYS LISTS */}
          <div className="plays-list">
            
            {activeTab === 'best' ? (
              bestScores.length === 0 ? (
                <div className="glass-panel" style={{ padding: '48px', fontStyle: 'italic', color: '#5d5a75', textAlign: 'center', fontSize: '0.8rem' }}>
                  No scores recorded yet. Play a chart inside the game client to submit a score!
                </div>
              ) : (
                bestScores.map((score, index) => {
                  const clearBadge = getClearBadge(score);
                  return (
                    <div 
                      key={index}
                      className="play-card glass-panel animate-fadeIn"
                      style={{ animationDelay: `${index * 0.03}s` }}
                    >
                      {/* Left: Rank index & Beatmap detail */}
                      <div className="play-card-left">
                        <span className="play-card-rank">
                          #{index + 1}
                        </span>
                        
                        <div className="play-card-info">
                          <h3 className="play-card-title">
                            {score.title}
                          </h3>
                          <p className="play-card-meta">
                            {score.artist}
                          </p>
                          
                          <div className="play-card-badges">
                            <span className={`difficulty-badge ${getDifficultyColor(score.difficulty)}`} style={{ fontSize: '0.65rem', fontWeight: 800 }}>
                              {score.difficulty} {score.level}
                            </span>
                            <span className={`clear-badge ${clearBadge.className}`}>
                              {clearBadge.text}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Right: Score, Grade, Play Volforce */}
                      <div className="play-card-right">
                        {/* Score and Grade */}
                        <div className="play-card-scores">
                          <span className="play-card-score">
                            {score.score.toLocaleString()}
                          </span>
                          <p className="play-card-combo">
                            Max Combo {score.maxCombo || 0}
                          </p>
                        </div>
                        
                        {/* Grade Badge */}
                        <span className={`grade-badge grade-${score.grade.replace('+', '_plus')}`}>
                          {score.grade}
                        </span>

                        {/* Calculated Play Volforce */}
                        <div className="play-card-vf-box">
                          <span className="play-card-vf text-[#00e5ff]">
                            {score.volforce.toFixed(3)}
                          </span>
                          <p className="play-card-vf-label">
                            Play VF
                          </p>
                        </div>

                      </div>

                    </div>
                  );
                })
              )
            ) : (
              recentScores.length === 0 ? (
                <div className="glass-panel" style={{ padding: '48px', fontStyle: 'italic', color: '#5d5a75', textAlign: 'center', fontSize: '0.8rem' }}>
                  No recent play attempts recorded.
                </div>
              ) : (
                recentScores.map((score, index) => {
                  const clearBadge = getClearBadge(score);
                  return (
                    <div 
                      key={index}
                      className="play-card glass-panel animate-fadeIn"
                      style={{ animationDelay: `${index * 0.02}s` }}
                    >
                      {/* Left: Beatmap detail */}
                      <div className="play-card-left">
                        <div className="play-card-info">
                          <h3 className="play-card-title">
                            {score.title}
                          </h3>
                          <p className="play-card-meta">
                            {score.artist}
                          </p>
                          
                          <div className="play-card-badges">
                            <span className={`difficulty-badge ${getDifficultyColor(score.difficulty)}`} style={{ fontSize: '0.65rem', fontWeight: 800 }}>
                              {score.difficulty} {score.level}
                            </span>
                            <span className={`clear-badge ${clearBadge.className}`}>
                              {clearBadge.text}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Right: Score, Grade, Play Volforce & Timestamp */}
                      <div className="play-card-right">
                        {/* Score and Grade */}
                        <div className="play-card-scores">
                          <span className="play-card-score">
                            {score.score.toLocaleString()}
                          </span>
                          <p className="play-card-combo" style={{ textTransform: 'none' }}>
                            {formatTimestamp(score.timestamp)}
                          </p>
                        </div>
                        
                        {/* Grade Badge */}
                        <span className={`grade-badge grade-${score.grade.replace('+', '_plus')}`}>
                          {score.grade}
                        </span>

                        {/* Calculated Play Volforce */}
                        <div className="play-card-vf-box">
                          <span className="play-card-vf text-[#ff007f]">
                            {score.volforce.toFixed(3)}
                          </span>
                          <p className="play-card-vf-label">
                            Play VF
                          </p>
                        </div>

                      </div>

                    </div>
                  );
                })
              )
            )}

          </div>

        </div>
      )}

    </div>
  );
}
