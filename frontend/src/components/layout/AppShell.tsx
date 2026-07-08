import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { BarChart3, MessagesSquare, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

const navItems = [
  { to: "/workspace", label: "Workspace", icon: MessagesSquare },
  { to: "/dashboard", label: "Dashboard", icon: BarChart3 },
];

export function AppShell() {
  const location = useLocation();
  const isLanding = location.pathname === "/";

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-40 border-b bg-card/80 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-[1500px] items-center justify-between px-4 lg:px-6">
          <Link to="/" className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary">
              <Sparkles className="h-4 w-4 text-primary-foreground" />
            </span>
            <span className="text-lg font-semibold tracking-tight">
              Converge<span className="text-primary">AI</span>
            </span>
          </Link>

          <nav className="flex items-center gap-1">
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to}>
                {({ isActive }) => (
                  <span
                    className={cn(
                      "flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                      isActive
                        ? "bg-accent text-accent-foreground"
                        : "text-muted-foreground hover:bg-muted hover:text-foreground",
                    )}
                  >
                    <item.icon className="h-4 w-4" />
                    {item.label}
                  </span>
                )}
              </NavLink>
            ))}
            {isLanding && (
              <Button asChild size="sm" className="ml-2">
                <Link to="/workspace">Get started</Link>
              </Button>
            )}
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>
    </div>
  );
}
