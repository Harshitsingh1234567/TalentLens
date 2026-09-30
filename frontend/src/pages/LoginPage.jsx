import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { ROLE_HOME_PATH } from '../auth/roles';
import { ApiError } from '../api/ApiError';
import { Alert } from '../components/Alert/Alert';
import { Button } from '../components/Button/Button';
import { Card } from '../components/Card/Card';
import { TextField } from '../components/TextField/TextField';
import { BrandMark } from '../components/BrandMark/BrandMark';
import styles from './AuthPage.module.css';

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState(() => (location.state && location.state.registeredEmail) || '');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});
    setIsSubmitting(true);

    try {
      const response = await login(email, password);
      const from = location.state && location.state.from ? location.state.from.pathname : null;
      navigate(from || ROLE_HOME_PATH[response.role], { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.isValidationError() && error.fieldErrors) {
        setFieldErrors(error.fieldErrors);
      } else if (error instanceof ApiError) {
        // The backend returns the same generic 401 message for both an
        // unknown email and a wrong password, by design - the frontend
        // simply surfaces it as-is rather than guessing which one it was.
        setFormError(error.message);
      } else {
        setFormError('Something went wrong. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={styles.page}>
      <Card className={styles.card}>
        <div className={styles.brand}>
          <span className={styles.brandMark}>
            <BrandMark size={26} />
          </span>
          <span className={styles.brandName}>TalentLens</span>
        </div>
        <p className={styles.tagline}>AI-Powered Resume &amp; Job Matching</p>

        <h1>Log in</h1>
        <p>Sign in to your TalentLens account.</p>

        {formError && <Alert tone="danger">{formError}</Alert>}

        <form onSubmit={handleSubmit} noValidate>
          <TextField
            label="Email"
            type="email"
            name="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            error={fieldErrors.email}
            required
          />
          <TextField
            label="Password"
            type="password"
            name="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={fieldErrors.password}
            required
          />
          <Button type="submit" isLoading={isSubmitting} className={styles.submit}>
            Log in
          </Button>
        </form>

        <p className={styles.switchLink}>
          Don't have an account? <Link to="/register">Create one</Link>
        </p>
      </Card>
    </div>
  );
}
