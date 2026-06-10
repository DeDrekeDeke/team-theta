import { FormEvent, useEffect, useState } from 'react';
import { Button } from '../../components/Button';
import { ErrorMessage } from '../../components/ErrorMessage';
import { FormField, TextInput } from '../../components/FormField';
import { LoadingState } from '../../components/LoadingState';
import { PageHeader } from '../../components/PageHeader';
import { formatDateTime } from '../../lib/formatters';
import { AdminUser, createUser, listUsers, updateUser } from './adminApi';

const emptyForm = {
  email: '',
  displayName: '',
  password: ''
};

export function UsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    listUsers()
      .then(setUsers)
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Could not load users'))
      .finally(() => setLoading(false));
  }, []);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setNotice('');

    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }

    setCreating(true);
    try {
      const created = await createUser(form);
      setUsers((current) => [...current, created].sort((a, b) => a.id - b.id));
      setForm(emptyForm);
      setNotice(`${created.email} was created.`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not create user');
    } finally {
      setCreating(false);
    }
  }

  async function handleRoleChange(user: AdminUser) {
    setError('');
    setNotice('');
    setSavingId(user.id);

    try {
      const updated = await updateUser(user.id, {
        email: user.email,
        displayName: user.displayName,
        admin: !user.admin
      });
      setUsers((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setNotice(`${updated.email} is now ${updated.admin ? 'an admin' : 'a regular user'}.`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not update user');
    } finally {
      setSavingId(null);
    }
  }

  return (
    <section className="page-section">
      <PageHeader title="Users" description="Create users and manage basic admin access." />

      {error ? <ErrorMessage message={error} /> : null}
      {notice ? <p className="notice-message">{notice}</p> : null}

      <form className="panel form-stack" onSubmit={handleCreate}>
        <div className="form-grid">
          <FormField label="Email" htmlFor="new-user-email">
            <TextInput
              id="new-user-email"
              type="email"
              value={form.email}
              onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
              required
            />
          </FormField>
          <FormField label="Display name" htmlFor="new-user-display-name">
            <TextInput
              id="new-user-display-name"
              value={form.displayName}
              onChange={(event) => setForm((current) => ({ ...current, displayName: event.target.value }))}
              required
            />
          </FormField>
          <FormField label="Password" htmlFor="new-user-password">
            <TextInput
              id="new-user-password"
              type="password"
              value={form.password}
              onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
              minLength={8}
              required
            />
          </FormField>
        </div>
        <div className="inline-actions end">
          <Button type="submit" disabled={creating}>
            {creating ? 'Creating...' : 'Create user'}
          </Button>
        </div>
      </form>

      {loading ? (
        <LoadingState />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Created</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => (
                <tr key={user.id}>
                  <td>{user.displayName}</td>
                  <td>{user.email}</td>
                  <td>
                    <span className={user.admin ? 'role-badge admin' : 'role-badge'}>
                      {user.admin ? 'Admin' : 'User'}
                    </span>
                  </td>
                  <td>{formatDateTime(user.createdAt)}</td>
                  <td>
                    <Button
                      type="button"
                      variant="secondary"
                      disabled={savingId === user.id}
                      onClick={() => handleRoleChange(user)}
                    >
                      {user.admin ? 'Make user' : 'Make admin'}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
