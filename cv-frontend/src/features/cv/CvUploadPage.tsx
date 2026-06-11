import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/Button';
import { ErrorMessage } from '../../components/ErrorMessage';
import { FormField, TextInput } from '../../components/FormField';
import { PageHeader } from '../../components/PageHeader';
import {
  compactErrors,
  MAX_TITLE_LENGTH,
  validateHtmlFile,
  validateOptionalTitle,
  validateOwnerUserId
} from '../../lib/validation';
import { uploadCv } from './cvApi';

export function CvUploadPage() {
  const navigate = useNavigate();
  const [ownerUserId, setOwnerUserId] = useState('2');
  const [title, setTitle] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationErrors = compactErrors([
      validateOwnerUserId(ownerUserId),
      validateOptionalTitle(title),
      validateHtmlFile(file)
    ]);

    if (validationErrors.length) {
      setError(validationErrors.join('\n'));
      return;
    }

    const data = new FormData();
    data.append('ownerUserId', ownerUserId.trim());
    data.append('title', title.trim());
    data.append('file', file as File);

    setLoading(true);
    setError('');
    try {
      const cv = await uploadCv(data);
      navigate(`/cvs/${cv.id}`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Upload failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-section narrow">
      <PageHeader title="Upload CV" description="Upload an HTML CV file to the AS-IS backend." />
      <form className="form-stack" onSubmit={handleSubmit} noValidate>
        <FormField label="Owner user id" htmlFor="ownerUserId">
          <TextInput
            id="ownerUserId"
            type="number"
            required
            min={1}
            step={1}
            value={ownerUserId}
            onChange={(event) => setOwnerUserId(event.target.value)}
          />
        </FormField>
        <FormField label="Title" htmlFor="title">
          <TextInput
            id="title"
            maxLength={MAX_TITLE_LENGTH}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </FormField>
        <FormField label="HTML file" htmlFor="file">
          <input
            id="file"
            className="file-input"
            type="file"
            required
            accept=".html,.htm,text/html"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
        </FormField>
        {error ? <ErrorMessage message={error} /> : null}
        <Button type="submit" disabled={loading}>
          {loading ? 'Uploading...' : 'Upload'}
        </Button>
      </form>
    </section>
  );
}
