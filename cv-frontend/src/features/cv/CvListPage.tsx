import { FormEvent, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../../components/Button';
import { ErrorMessage } from '../../components/ErrorMessage';
import { LoadingState } from '../../components/LoadingState';
import { PageHeader } from '../../components/PageHeader';
import { getCurrentUser } from '../auth/authStore';
import { CvTable } from './components/CvTable';
import { Cv, listCvs, searchCvs } from './cvApi';

function filterCvsForCurrentUser(cvs: Cv[]) {
  const user = getCurrentUser();

  if (!user) {
    return [];
  }

  if (user.admin) {
    return cvs;
  }

  return cvs.filter((cv) => cv.ownerUserId === user.userId);
}

export function CvListPage() {
  const [cvs, setCvs] = useState<Cv[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadCvs();
  }, []);

  async function loadCvs() {
    setLoading(true);
    setError('');
    try {
      setCvs(filterCvsForCurrentUser(await listCvs()));
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not load CVs');
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const loadedCvs = query.trim() ? await searchCvs(query) : await listCvs();
      setCvs(filterCvsForCurrentUser(loadedCvs));
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Search failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="page-section">
      <PageHeader
        title="CVs"
        description="AS-IS list of all CVs returned by the backend."
        actions={<Link className="button primary" to="/upload">Upload CV</Link>}
      />

      <form className="toolbar" onSubmit={handleSearch}>
        <input
          className="text-input"
          placeholder="Search title, owner, or file path"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
        <Button type="submit" variant="secondary">
          Search
        </Button>
      </form>

      {error ? <ErrorMessage message={error} /> : null}
      {loading ? <LoadingState /> : <CvTable cvs={cvs} />}
    </section>
  );
}
