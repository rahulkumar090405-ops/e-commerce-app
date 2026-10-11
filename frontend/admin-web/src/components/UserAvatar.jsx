import { useState } from "react";

export default function UserAvatar({ user }) {
  const [failedPicture, setFailedPicture] = useState(null);
  const label = user.name || user.firstName || user.email || "User";
  return <span className="avatar" aria-label={`${label}'s avatar`}>
    {user.picture && failedPicture !== user.picture
      ? <img src={user.picture} alt="" referrerPolicy="no-referrer" onError={() => setFailedPicture(user.picture)} />
      : label.charAt(0).toUpperCase()}
  </span>;
}
