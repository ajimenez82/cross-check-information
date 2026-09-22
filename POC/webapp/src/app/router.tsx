import { createBrowserRouter } from 'react-router-dom';
import { App } from './App';
import { HomePage } from '../features/home/HomePage';
import { AnalysisPage } from '../features/analysis/AnalysisPage';

export const router = createBrowserRouter([{ element: <App />, children: [
  { path: '/', element: <HomePage /> },
  { path: '/resultados', element: <AnalysisPage /> },
  { path: '/resultados/:id', element: <AnalysisPage /> },
  { path: '*', element: <HomePage /> },
] }]);
