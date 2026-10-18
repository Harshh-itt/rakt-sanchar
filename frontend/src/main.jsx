import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { IdentityProvider } from './context/IdentityContext';
import { LanguageProvider } from './context/LanguageContext';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <IdentityProvider>
        <LanguageProvider>
          <App />
        </LanguageProvider>
      </IdentityProvider>
    </BrowserRouter>
  </React.StrictMode>
);
