'use client';

import { useAuth } from '@/contexts/AuthContext';
import { useTheme } from '@/components/ThemeProvider';
import { getGreeting } from '@/lib/utils';
import { Sun, Moon, Bell } from 'lucide-react';
import { Button } from '@/components/ui/button';

export function Header() {
  const { profile } = useAuth();
  const { resolvedTheme, setTheme, theme } = useTheme();

  const greeting = getGreeting();
  const firstName = profile?.full_name?.split(' ')[0] || 'there';

  const cycleTheme = () => {
    if (theme === 'light') setTheme('dark');
    else if (theme === 'dark') setTheme('system');
    else setTheme('light');
  };

  return (
    <header className="sticky top-0 z-30 h-[var(--header-height)] border-b border-border bg-card/80 backdrop-blur-xl">
      <div className="flex items-center justify-between h-full px-4 lg:px-6">
        {/* Mobile logo */}
        <div className="lg:hidden flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-primary flex items-center justify-center">
            <span className="text-xs font-bold text-primary-foreground">B</span>
          </div>
          <span className="font-bold text-sm">BudgetWise</span>
        </div>

        {/* Desktop greeting */}
        <div className="hidden lg:block">
          <h1 className="text-sm font-medium text-foreground">
            {greeting}, <span className="font-semibold">{firstName}</span>
          </h1>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon-sm"
            onClick={cycleTheme}
            title={`Current theme: ${theme}`}
          >
            {resolvedTheme === 'dark' ? (
              <Moon className="w-4 h-4" />
            ) : (
              <Sun className="w-4 h-4" />
            )}
          </Button>
          <Button variant="ghost" size="icon-sm" title="Notifications">
            <Bell className="w-4 h-4" />
          </Button>
          <div className="lg:hidden w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center text-xs font-semibold text-primary ml-1">
            {profile?.full_name?.charAt(0)?.toUpperCase() || '?'}
          </div>
        </div>
      </div>
    </header>
  );
}
