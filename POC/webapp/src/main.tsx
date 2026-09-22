import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { router } from './app/router';
import { ConversationProvider } from './features/conversations/ConversationProvider';
import './shared/styles/global.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><ConversationProvider><RouterProvider router={router} /></ConversationProvider></React.StrictMode>,
);
