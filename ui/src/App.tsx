import { Routes, Route } from 'react-router-dom';
import { JobListPage } from './pages/JobListPage';
import { JobDetailPage } from './pages/JobDetailPage';
import { RunDetailPage } from './pages/RunDetailPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<JobListPage />} />
      <Route path="/jobs/:jobFamilyId" element={<JobDetailPage />} />
      <Route path="/runs/:runId" element={<RunDetailPage />} />
    </Routes>
  );
}
