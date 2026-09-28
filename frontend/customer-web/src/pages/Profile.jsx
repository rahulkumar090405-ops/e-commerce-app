import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getMyProfile, updateMyProfile } from "../services/profileApi";

const emptyProfile = { firstName: "", lastName: "", gender: "", mobile: "" };

export default function Profile() {
  const { user, saveProfile } = useAuth();
  const [profile, setProfile] = useState(() => ({ ...emptyProfile, ...user }));
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!user?.token) return;
    getMyProfile(user.token)
      .then((data) => { setProfile(data); saveProfile(data); })
      .catch((requestError) => setError(requestError.message || "Unable to load your profile."))
      .finally(() => setLoading(false));
  }, [user?.token, saveProfile]);

  if (!user) return <Navigate to="/login" replace />;

  function change(event) {
    setProfile((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault(); setError(""); setMessage(""); setSaving(true);
    try {
      const updated = await updateMyProfile(profile, user.token);
      setProfile(updated); saveProfile(updated); setMessage("Profile updated successfully.");
    } catch (requestError) {
      setError(requestError.message || "Unable to update your profile.");
    } finally { setSaving(false); }
  }

  return <main className="profile-page"><div className="profile-intro"><p className="eyebrow">YOUR ACCOUNT</p><h1>Manage your<br />profile.</h1><p>Keep your contact details up to date for future orders and delivery updates.</p><span aria-hidden="true">✳</span></div>
    <section className="profile-content"><p className="eyebrow">PERSONAL DETAILS</p><h2>My profile</h2><p>Update the information held in user-service.</p>
      {loading ? <p className="profile-loading">Loading your profile…</p> : <form className="profile-form" onSubmit={submit}>
        <div className="form-row"><div><label htmlFor="firstName">First name</label><input id="firstName" name="firstName" value={profile.firstName || ""} onChange={change} required /></div><div><label htmlFor="lastName">Last name</label><input id="lastName" name="lastName" value={profile.lastName || ""} onChange={change} required /></div></div>
        <label htmlFor="email">Email address</label><input id="email" value={profile.email || ""} readOnly aria-readonly="true" />
        <label htmlFor="gender">Gender</label><select id="gender" name="gender" value={profile.gender || ""} onChange={change} required><option value="" disabled>Select gender</option><option value="Male">Male</option><option value="Female">Female</option><option value="Other">Other</option><option value="Prefer not to say">Prefer not to say</option></select>
        <label htmlFor="mobile">Mobile number</label><input id="mobile" name="mobile" type="tel" value={profile.mobile || ""} onChange={change} inputMode="tel" pattern="[+0-9]{7,15}" required />
        {error && <p className="form-error" role="alert">{error}</p>}{message && <p className="form-success" role="status">{message}</p>}
        <button className="primary" type="submit" disabled={saving}>{saving ? "Saving…" : "Save profile"}<span>→</span></button>
      </form>}
    </section>
  </main>;
}
