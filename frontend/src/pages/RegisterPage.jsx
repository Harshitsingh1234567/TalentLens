import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { ROLES, ROLE_OPTIONS } from '../auth/roles';
import { ApiError } from '../api/ApiError';
import { Alert } from '../components/Alert/Alert';
import { Button } from '../components/Button/Button';
import { Card } from '../components/Card/Card';
import { TextField } from '../components/TextField/TextField';
import { SelectField } from '../components/TextField/SelectField';
import { BrandMark } from '../components/BrandMark/BrandMark';
import styles from './AuthPage.module.css';

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState(ROLES.JOB_SEEKER);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setFormError(null);
    setFieldErrors({});
    setIsSubmitting(true);

    try {
      await register(email, password, role);
      // POST /api/auth/register returns the created account, not a
      // token - a separate login call is genuinely required, matching
      // the backend's actual contract (see AuthService.register vs
      // .login). Redirecting to /login with the email pre-filled avoids
      // making the person retype it immediately after choosing it.
      navigate('/login', { replace: true, state: { registeredEmail: email } });
    } catch (error) {
      if (error instanceof ApiError && error.isValidationError() && error.fieldErrors) {
        setFieldErrors(error.fieldErrors);
      } else if (error instanceof ApiError) {
        // Covers 409 (email already registered) and anything else the
        // backend reports as a plain message.
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

        <h1>Create an account</h1>
        <p>Get started with TalentLens.</p>

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
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={fieldErrors.password}
            hint={!fieldErrors.password ? 'At least 8 characters.' : undefined}
            required
          />
          <SelectField
            label="I am a…"
            name="role"
            value={role}
            onChange={(event) => setRole(event.target.value)}
            options={ROLE_OPTIONS}
            error={fieldErrors.role}
          />
          <Button type="submit" isLoading={isSubmitting} className={styles.submit}>
            Create account
          </Button>
        </form>

        <p className={styles.switchLink}>
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </Card>
    </div>
  );
}
