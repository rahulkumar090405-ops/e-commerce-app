import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { getCurrentUser } from "../services/authApi";
import { updateMyProfile } from "../services/userApi";

const emptyProfile = { firstName: "", lastName: "", gender: "", mobile: "" };

export default function Profile() {
  const { user, saveCurrentUser } = useAuth();
  const [profile, setProfile] = useState(() => ({ ...emptyProfile, ...user }));
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getCurrentUser().then((data) => {
      setProfile((current) => ({ ...current, ...data }));
      saveCurrentUser(data);
    }).catch((requestError) => setError(requestError.message || "Unable to load your profile."));
  }, [saveCurrentUser]);

  function change(event) {
    setProfile((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault(); setError(""); setMessage(""); setSaving(true);
    try {
      const updated = await updateMyProfile(profile);
      setProfile(updated); saveCurrentUser(updated); setMessage("Profile updated successfully.");
    } catch (requestError) {
      setError(requestError.message || "Unable to update your profile.");
    } finally { setSaving(false); }
  }

  return <main className="dashboard"><div className="page-heading"><div><p className="eyebrow">ACCOUNT SETTINGS</p><h1>My profile</h1><p className="muted">Update your personal contact details. Roles are managed separately.</p></div></div>
    <section className="panel profile-panel"><form className="profile-form" onSubmit={submit}>
      <div className="form-row"><div><label htmlFor="firstName">First name</label><input id="firstName" name="firstName" value={profile.firstName || ""} onChange={change} required /></div><div><label htmlFor="lastName">Last name</label><input id="lastName" name="lastName" value={profile.lastName || ""} onChange={change} required /></div></div>
      <label htmlFor="email">Email address</label><input id="email" value={profile.email || ""} readOnly aria-readonly="true" />
      <label htmlFor="gender">Gender</label><select id="gender" name="gender" value={profile.gender || ""} onChange={change} required><option value="" disabled>Select gender</option><option value="Male">Male</option><option value="Female">Female</option><option value="Other">Other</option><option value="Prefer not to say">Prefer not to say</option></select>
      <label htmlFor="mobile">Mobile number</label><input id="mobile" name="mobile" type="tel" value={profile.mobile || ""} onChange={change} inputMode="tel" pattern="[+0-9]{7,15}" required />
      <div className="profile-roles"><span>Your roles</span><div className="role-list">{(profile.roles || []).map((role) => <span className="role-badge" key={role}>{role.replace("ROLE_", "")}</span>)}</div></div>
      {error && <p className="form-error" role="alert">{error}</p>}{message && <p className="form-success" role="status">{message}</p>}
      <button className="primary profile-submit" type="submit" disabled={saving}>{saving ? "Saving…" : "Save profile"}<span>→</span></button>
    </form></section>
  </main>;
}
