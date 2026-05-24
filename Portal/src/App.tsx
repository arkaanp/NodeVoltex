import { useState } from 'react';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Home from './pages/Home';
import Leaderboard from './pages/Leaderboard';
import Profile from './pages/Profile';
import Beatmaps from './pages/Beatmaps';

export default function App() {
  const [currentTab, setCurrentTab] = useState<string>('home');
  const [selectedUsername, setSelectedUsername] = useState<string>('');

  const handleSearchUser = (username: string) => {
    setSelectedUsername(username);
    setCurrentTab('profile');
  };

  const handleSelectLeaderboardUser = (username: string) => {
    setSelectedUsername(username);
    setCurrentTab('profile');
  };

  const renderContent = () => {
    switch (currentTab) {
      case 'home':
        return <Home setCurrentTab={setCurrentTab} />;
      case 'leaderboard':
        return <Leaderboard onSelectUser={handleSelectLeaderboardUser} />;
      case 'profile':
        return <Profile username={selectedUsername} />;
      case 'beatmaps':
        return <Beatmaps />;
      default:
        return <Home setCurrentTab={setCurrentTab} />;
    }
  };

  return (
    <div className="app-container">
      {/* NAVBAR */}
      <Navbar 
        currentTab={currentTab} 
        setCurrentTab={setCurrentTab} 
        onSearchUser={handleSearchUser}
      />

      {/* DYNAMIC MAIN CONTENT */}
      <main className="app-main">
        {renderContent()}
      </main>

      {/* FOOTER */}
      <Footer />
    </div>
  );
}
