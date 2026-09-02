import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
// 🌟 關鍵修正：把 ../ 換成 ./，全部使用正斜線
import GameLobbyPage from './pages/Lobby/GameLobbyPage.jsx' 

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <GameLobbyPage />
  </StrictMode>,
)