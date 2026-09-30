import { classNames } from '../../utils/classNames';
import styles from './SkillTagList.module.css';

/**
 * Renders a set of skill strings (matchedSkills or missingSkills from
 * MatchResponse) as scannable chips instead of a comma-joined sentence.
 * `tone` only changes color semantics (matched = positive, missing =
 * neutral/attention) - the data itself is never altered or invented.
 */
export function SkillTagList({ skills, tone = 'neutral', emptyLabel }) {
  if (!skills || skills.length === 0) {
    return emptyLabel ? <p className={styles.empty}>{emptyLabel}</p> : null;
  }

  return (
    <ul className={styles.list}>
      {skills.map((skill) => (
        <li key={skill} className={classNames(styles.tag, styles[tone])}>
          {skill}
        </li>
      ))}
    </ul>
  );
}
