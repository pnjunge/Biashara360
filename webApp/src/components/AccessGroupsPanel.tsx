import React, { useState } from 'react'
import { AccessConfig, AccessGroup, UserResponse, accessApi } from '../services/api'
import { Card, Btn, Input } from './ui'

export default function AccessGroupsPanel({ config, users, businessId, onSaved }: {
  config: AccessConfig; users: UserResponse[]; businessId?: string; onSaved: () => void
}) {
  const empty = () => ({ name: '', description: '', allowedMenus: [] as string[], roleIds: [] as string[], isActive: true })
  const [draft, setDraft] = useState(empty)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [open, setOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [memberIds, setMemberIds] = useState<string[]>([])
  const toggle = (values: string[], id: string) => values.includes(id) ? values.filter(v => v !== id) : [...values, id]
  const edit = (group?: AccessGroup) => {
    setEditingId(group?.id || null)
    setDraft(group ? { name: group.name, description: group.description, allowedMenus: group.allowedMenus,
      roleIds: group.roleIds || [], isActive: group.isActive } : empty())
    setMemberIds(group?.userIds || [])
    setMessage(''); setOpen(true)
  }
  const save = async () => {
    setSaving(true); setMessage('')
    try {
      const response = editingId ? await accessApi.updateGroup(editingId, draft, businessId) : await accessApi.createGroup(draft, businessId)
      if (!response.success || !response.data) throw new Error(response.message || 'Could not save group.')
      setEditingId(response.data.id)
      const members = await accessApi.assignUsers(response.data.id, memberIds, businessId)
      if (!members.success) throw new Error(members.message || 'Group saved, but members could not be updated.')
      setOpen(false); setMessage('Group rights and members saved.'); onSaved()
    } catch (error: any) {
      setMessage(error.response?.data?.message || error.message || 'Could not save group.')
      onSaved()
    } finally { setSaving(false) }
  }
  return <Card style={{ padding: 20 }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
      <h3>Access Groups</h3><Btn small disabled={saving} onClick={() => edit()}>Create Group</Btn>
    </div>
    <p>Assign permission roles to any group. Members inherit its active roles, including Financial &amp; Profit P&amp;L access.</p>
    {message && <p role="status">{message}</p>}
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 12 }}>
      {config.groups.map(group => <Btn small variant="secondary" key={group.id} disabled={saving} onClick={() => edit(group)}>
        {group.name} · {group.userIds.length} members{!group.isActive ? ' (disabled)' : ''}
      </Btn>)}
    </div>
    {open && <fieldset disabled={saving} style={{ border: 0, padding: 0, marginTop: 16 }}>
      <h4>{editingId ? 'Edit Group' : 'Create Group'}</h4>
      <Input label="Group Name" value={draft.name} onChange={name => setDraft({ ...draft, name })} />
      <Input label="Description" value={draft.description} onChange={description => setDraft({ ...draft, description })} />
      <label><input type="checkbox" checked={draft.isActive} onChange={e => setDraft({ ...draft, isActive: e.target.checked })} /> Active</label>
      <h4>Permission Roles</h4>
      <p>Configure each role’s rights below. Menus control navigation; roles grant specific permissions.</p>
      {config.roles.length === 0 && <p>Create a custom role to define the rights this group needs.</p>}
      {config.roles.map(role => <label key={role.id} style={{ display: 'block', marginTop: 6 }}>
        <input type="checkbox" checked={draft.roleIds.includes(role.id)} onChange={() => setDraft({ ...draft, roleIds: toggle(draft.roleIds, role.id) })} />
        {' '}{role.name} · {role.permissions?.length || 0} rights{!role.isActive ? ' (disabled)' : ''}
      </label>)}
      <h4>Additional Menus</h4>
      <div className="responsive-grid responsive-grid-3">
        {config.menus.map(menu => <label key={menu.key}>
          <input type="checkbox" checked={draft.allowedMenus.includes(menu.key)} onChange={() => setDraft({ ...draft, allowedMenus: toggle(draft.allowedMenus, menu.key) })} />
          {' '}{menu.label}{!config.enabledMenus.includes(menu.key) ? ' (disabled for business)' : ''}
        </label>)}
      </div>
      <h4>Members</h4>
      {users.map(user => <label key={user.id} style={{ display: 'block', marginTop: 6 }}>
        <input type="checkbox" checked={memberIds.includes(user.id)} onChange={() => setMemberIds(toggle(memberIds, user.id))} /> {user.name}
      </label>)}
      <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
        <Btn disabled={saving || draft.name.trim().length < 2} onClick={save}>{saving ? 'Saving…' : 'Save Group'}</Btn>
        <Btn variant="secondary" onClick={() => setOpen(false)}>Cancel</Btn>
      </div>
    </fieldset>}
  </Card>
}
