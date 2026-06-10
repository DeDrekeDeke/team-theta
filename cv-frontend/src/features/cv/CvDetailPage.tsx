import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button } from '../../components/Button';
import { ErrorMessage } from '../../components/ErrorMessage';
import { FormField, TextInput } from '../../components/FormField';
import { LoadingState } from '../../components/LoadingState';
import { PageHeader } from '../../components/PageHeader';
import { AiActionPanel } from '../ai/AiActionPanel';
import { getCurrentUser } from '../auth/authStore';
import { archiveCv, Cv, getCv, updateCv } from './cvApi';

export function CvDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [cv, setCv] = useState<Cv | null>(null);
  const [title, setTitle] = useState('');
  const [summary, setSummary] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [archiving, setArchiving] = useState(false);

  useEffect(() => {
    if (!id) {
      return;
    }

    getCv(id)
      .then((loadedCv) => {
        const user = getCurrentUser();

        if (!user) {
          throw new Error('Log in to view this CV');
        }

        if (!user.admin && loadedCv.ownerUserId !== user.userId) {
          throw new Error('You can only view your own CVs');
        }

        setCv(loadedCv);
        setTitle(loadedCv.title);
        setSummary(loadedCv.summary ?? '');
      })
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Could not load CV'));
  }, [id]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!cv) {
      return;
    }

    setSaving(true);
    setError('');

    try {
      const updatedCv = await updateCv(cv.id, {
        title,
        summary
      });
      setCv(updatedCv);
      setTitle(updatedCv.title);
      setSummary(updatedCv.summary ?? '');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not update CV');
    } finally {
      setSaving(false);
    }
  }

  async function handleArchive() {
    if (!cv) {
      return;
    }

    setArchiving(true);
    setError('');

    try {
      await archiveCv(cv.id);
      navigate('/');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not archive CV');
      setArchiving(false);
    }
  }

  if (error) {
    return <ErrorMessage message={error} />;
  }

  if (!cv) {
    return <LoadingState />;
  }

  return (
    <section className="page-section">
      <PageHeader title={cv.title} description={`Owner: ${cv.ownerEmail}`} />

      <form className="form-stack" onSubmit={handleSave}>
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

        <div className="toolbar">
          <Button type="submit" disabled={saving || !title.trim()}>
            {saving ? 'Saving...' : 'Save changes'}
          </Button>
          <Button type="button" variant="secondary" disabled={archiving} onClick={handleArchive}>
            {archiving ? 'Archiving...' : 'Archive CV'}
          </Button>
        </div>
      </form>

      <div className="detail-grid">
        <div className="panel">
          <div className="panel-header">
            <h3>CV Preview</h3>
          </div>
          <div className="cv-preview">
            <h2>{cv.title}</h2>
            {cv.summary ? <p>{cv.summary}</p> : <p className="muted">No summary added yet.</p>}
          </div>
        </div>
        <AiActionPanel cvId={cv.id} />
      </div>
    </section>
  );
}
