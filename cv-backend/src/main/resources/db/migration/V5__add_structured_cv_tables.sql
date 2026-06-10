ALTER TABLE cv
ADD COLUMN summary TEXT;

ALTER TABLE cv
ALTER COLUMN uploaded_html_file_path DROP NOT NULL;

CREATE TABLE cv_personal_details (
    id BIGSERIAL PRIMARY KEY,
    cv_id BIGINT NOT NULL UNIQUE REFERENCES cv(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    location VARCHAR(255),
    headline VARCHAR(255)
);

CREATE TABLE cv_education_entry (
    id BIGSERIAL PRIMARY KEY,
    cv_id BIGINT NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    institution VARCHAR(255) NOT NULL,
    degree VARCHAR(255),
    field_of_study VARCHAR(255),
    start_date DATE,
    end_date DATE,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_education_entry_cv_id ON cv_education_entry(cv_id);

CREATE TABLE cv_work_experience_entry (
    id BIGSERIAL PRIMARY KEY,
    cv_id BIGINT NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    employer VARCHAR(255) NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_work_experience_entry_cv_id ON cv_work_experience_entry(cv_id);

CREATE TABLE cv_skill (
    id BIGSERIAL PRIMARY KEY,
    cv_id BIGINT NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255),
    proficiency VARCHAR(255),
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_skill_cv_id ON cv_skill(cv_id);

CREATE TABLE cv_language (
    id BIGSERIAL PRIMARY KEY,
    cv_id BIGINT NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    proficiency VARCHAR(255),
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_language_cv_id ON cv_language(cv_id);

CREATE TABLE cv_link (
    id BIGSERIAL PRIMARY KEY,
    cv_id BIGINT NOT NULL REFERENCES cv(id) ON DELETE CASCADE,
    label VARCHAR(255) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_cv_link_cv_id ON cv_link(cv_id);
