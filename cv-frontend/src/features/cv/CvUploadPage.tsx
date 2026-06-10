import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/Button';
import { ErrorMessage } from '../../components/ErrorMessage';
import { FormField, TextInput } from '../../components/FormField';
import { PageHeader } from '../../components/PageHeader';
import { getCurrentUser } from '../auth/authStore';
import { createCv } from './cvApi';

export function CvUploadPage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [summary, setSummary] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const user = getCurrentUser();

    if (!user) {
      setError('Log in to create a CV.');
      return;
    }

    if (!title.trim()) {
      setError('Add a title first.');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const cv = await createCv({
        ownerUserId: user.userId,
        title,
        summary
      });
      navigate(`/cvs/${cv.id}`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not create CV');
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-section narrow">
      <PageHeader title="Create CV" description="Create a structured CV without uploading HTML." />
      <form className="form-stack" onSubmit={handleSubmit}>
        <FormField label="Title" htmlFor="title">
          <TextInput id="title" value={title} onChange={(event) => setTitle(event.target.value)} />
        </FormField>
        <label className="form-field" htmlFor="summary">
          <span>Summary</span>
          <textarea
            id="summary"
            className="text-input"
            rows={6}
            value={summary}
            onChange={(event) => setSummary(event.target.value)}
          />
        </label>
        {error ? <ErrorMessage message={error} /> : null}
        <Button type="submit" disabled={loading || !title.trim()}>
          {loading ? 'Creating...' : 'Create CV'}
        </Button>
      </form>
    </section>
  );
}
