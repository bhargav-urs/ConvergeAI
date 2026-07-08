import { Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/layout/AppShell";
import LandingPage from "@/pages/LandingPage";
import WorkspacePage from "@/pages/WorkspacePage";
import DashboardPage from "@/pages/DashboardPage";

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<LandingPage />} />
        <Route path="/workspace" element={<WorkspacePage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
      </Route>
    </Routes>
  );
}
