import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/Button';
import { ErrorMessage } from '../../components/ErrorMessage';
import { FormField, TextInput } from '../../components/FormField';
import { PageHeader } from '../../components/PageHeader';
import { compactErrors, validateEmail, validatePassword } from '../../lib/validation';
import { login } from './authApi';
import { saveCurrentUser } from './authStore';

export function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('alice@example.com');
  const [password, setPassword] = useState('user123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationErrors = compactErrors([
      validateEmail(email),
      validatePassword(password)
    ]);

    if (validationErrors.length) {
      setError(validationErrors.join('\n'));
      return;
    }

    setLoading(true);
    setError('');

    try {
      const user = await login({ email, password });
      saveCurrentUser(user);
      navigate('/');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-section narrow">
      <PageHeader title="Log in" description="Use one of the seeded demo users." />
      <form className="form-stack" onSubmit={handleSubmit} noValidate>
        <FormField label="Email" htmlFor="email">
          <TextInput
            id="email"
            type="email"
            required
            maxLength={255}
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </FormField>
        <FormField label="Password" htmlFor="password">
          <TextInput
            id="password"
            type="password"
            required
            minLength={6}
            maxLength={255}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </FormField>
        {error ? <ErrorMessage message={error} /> : null}
        <Button type="submit" disabled={loading}>
          {loading ? 'Logging in...' : 'Log in'}
        </Button>
      </form>
    </section>
  );
}
