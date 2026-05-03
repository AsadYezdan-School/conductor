import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Amplify } from 'aws-amplify';
import {
  confirmSignIn,
  getCurrentUser,
  signIn as amplifySignIn,
  signOut as amplifySignOut,
  type SignInOutput,
} from 'aws-amplify/auth';
import { LoginPage } from './LoginPage';

interface AuthContextValue {
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

type AuthState = 'loading' | 'unauthenticated' | 'new-password-required' | 'authenticated' | 'no-config';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>('loading');
  const [credentials, setCredentials] = useState<{ email: string; password: string } | null>(null);

  useEffect(() => {
    async function init() {
      try {
        const res = await fetch('/config.json');
        if (!res.ok) { setState('no-config'); return; }
        const config: { userPoolId?: string; userPoolClientId?: string; region?: string } = await res.json();
        if (!config.userPoolId) { setState('no-config'); return; }
        Amplify.configure({
          Auth: {
            Cognito: {
              userPoolId: config.userPoolId,
              userPoolClientId: config.userPoolClientId ?? '',
            },
          },
        });
        try {
          await getCurrentUser();
          setState('authenticated');
        } catch {
          setState('unauthenticated');
        }
      } catch {
        setState('no-config');
      }
    }
    init();
  }, []);

  const handleSignIn = useCallback(async (email: string, password: string) => {
    setCredentials({ email, password });
    const result: SignInOutput = await amplifySignIn({ username: email, password });
    if (result.nextStep.signInStep === 'CONFIRM_SIGN_IN_WITH_NEW_PASSWORD_REQUIRED') {
      setState('new-password-required');
    } else if (result.isSignedIn) {
      setState('authenticated');
    }
  }, []);

  const handleNewPassword = useCallback(async (newPassword: string) => {
    await confirmSignIn({ challengeResponse: newPassword });
    setState('authenticated');
  }, []);

  const signOut = useCallback(async () => {
    await amplifySignOut();
    setCredentials(null);
    setState('unauthenticated');
  }, []);

  const noOpSignOut = useCallback(async () => {}, []);

  if (state === 'loading') {
    return <div className="flex min-h-screen items-center justify-center text-gray-500">Loading…</div>;
  }

  if (state === 'no-config') {
    return (
      <AuthContext.Provider value={{ signOut: noOpSignOut }}>
        {children}
      </AuthContext.Provider>
    );
  }

  if (state === 'unauthenticated') {
    return <LoginPage onSignIn={handleSignIn} />;
  }

  if (state === 'new-password-required') {
    return (
      <LoginPage
        onSignIn={handleSignIn}
        onNewPassword={handleNewPassword}
        newPasswordRequired
        email={credentials?.email ?? ''}
      />
    );
  }

  return (
    <AuthContext.Provider value={{ signOut }}>
      {children}
    </AuthContext.Provider>
  );
}
