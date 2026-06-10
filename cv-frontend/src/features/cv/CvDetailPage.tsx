import { FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button } from '../../components/Button';
import { ErrorMessage } from '../../components/ErrorMessage';
import { FormField, TextInput } from '../../components/FormField';
import { LoadingState } from '../../components/LoadingState';
import { PageHeader } from '../../components/PageHeader';
import { MAX_TITLE_LENGTH, validateRequiredTitle } from '../../lib/validation';
import { AiActionPanel } from '../ai/AiActionPanel';
import { getCurrentUser } from '../auth/authStore';
import { archiveCv, Cv, getCv, getCvHtml, getCvHtmlUrl, updateCv } from './cvApi';

export function CvDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [cv, setCv] = useState<Cv | null>(null);
  const [html, setHtml] = useState('');
  const [title, setTitle] = useState('');
  const [loadError, setLoadError] = useState('');
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [archiving, setArchiving] = useState(false);

  useEffect(() => {
    if (!id) {
      return;
    }

    getCv(id)
      .then(async (loadedCv) => {
        const user = getCurrentUser();

        if (!user) {
          throw new Error('Log in to view this CV');
        }

        if (!user.admin && loadedCv.ownerUserId !== user.userId) {
          throw new Error('You can only view your own CVs');
        }

        const loadedHtml = await getCvHtml(id);
        setCv(loadedCv);
        setTitle(loadedCv.title);
        setHtml(loadedHtml);
      })
      .catch((exception) => setLoadError(exception instanceof Error ? exception.message : 'Could not load CV'));
  }, [id]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!cv) {
      return;
    }

    const validationError = validateRequiredTitle(title);
    if (validationError) {
      setFormError(validationError);
      return;
    }

    setSaving(true);
    setFormError('');

    try {
      const updatedCv = await updateCv(cv.id, {
        title: title.trim(),
        uploadedHtmlFilePath: cv.uploadedHtmlFilePath
      });
      setCv(updatedCv);
      setTitle(updatedCv.title);
    } catch (exception) {
      setFormError(exception instanceof Error ? exception.message : 'Could not update CV');
    } finally {
      setSaving(false);
    }
  }

  async function handleArchive() {
    if (!cv) {
      return;
    }

    setArchiving(true);
    setFormError('');

    try {
      await archiveCv(cv.id);
      navigate('/');
    } catch (exception) {
      setFormError(exception instanceof Error ? exception.message : 'Could not archive CV');
      setArchiving(false);
    }
  }

  if (loadError) {
    return <ErrorMessage message={loadError} />;
  }

  if (!cv) {
    return <LoadingState />;
  }

  return (
    <section className="page-section">
      <PageHeader title={cv.title} description={`Owner: ${cv.ownerEmail}`} />

      <form className="form-stack" onSubmit={handleSave} noValidate>
        <FormField label="Title" htmlFor="title">
          <TextInput
            id="title"
            required
            maxLength={MAX_TITLE_LENGTH}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </FormField>

        {formError ? <ErrorMessage message={formError} /> : null}

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
            <h3>Uploaded HTML Content</h3>
            <a className="button secondary" href={getCvHtmlUrl(cv.id)} target="_blank" rel="noreferrer">
              Open raw HTML
            </a>
          </div>
          <iframe
            className="html-preview-frame"
            title="Uploaded CV preview"
            sandbox=""
            srcDoc={html}
          />
        </div>
        <AiActionPanel cvId={cv.id} />
      </div>
    </section>
  );
}
