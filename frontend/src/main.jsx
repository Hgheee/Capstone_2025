import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App.jsx";

import "./styles/globals.css";

import AuthProvider from "./contexts/AuthContext.jsx";

// ✅ 네이버 지도 API 미리 로드 (지도 로딩 속도 개선)
import { loadNaverMapScript } from "./utils/naverMapLoader";

// 앱 시작 시 네이버 지도 API 미리 로드 (백그라운드)
loadNaverMapScript().catch((err) => {
  console.warn("네이버 지도 API 사전 로드 실패 (지도 사용 시 다시 시도됩니다):", err);
});

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
