export function formatUserName(user: any): string {
  if (!user) return 'Guest User';
  const firstName = user.firstName || user.first_name || user.given_name;
  const lastName = user.lastName || user.last_name || user.family_name;
  
  if (firstName && lastName) return `${firstName} ${lastName}`;
  if (firstName) return firstName;
  if (user.name) return user.name;
  if (user.sub) {
    const emailPrefix = user.sub.split('@')[0];
    return emailPrefix
      .split('.')
      .map((part: string) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
  return 'Guest User';
}

export function extractInitials(name: string): string {
  if (!name || name === 'Guest User') return 'U';
  const nameParts = name.trim().split(' ').filter(part => part.length > 0);
  if (nameParts.length === 0) return 'U';
  if (nameParts.length === 1) return nameParts[0].charAt(0).toUpperCase();
  return (nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)).toUpperCase();
}

export function loadSidebarCollapseState(): boolean {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem('pcenter_sidebar_collapsed') === 'true';
  }
  return false;
}

export function saveSidebarCollapseState(isCollapsed: boolean): void {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('pcenter_sidebar_collapsed', String(isCollapsed));
  }
}
