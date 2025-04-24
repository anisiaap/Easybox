import React from 'react'
import styled from 'styled-components'

const AdminSettingsContainer = styled.div`
  padding: 20px;
  max-width: 600px;
`

const FormGroup = styled.div`
  margin-bottom: 16px;
`

const Label = styled.label`
  display: block;
  font-weight: 500;
  margin-bottom: 6px;
`

const Input = styled.input`
  width: 100%;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
`

const TextArea = styled.textarea`
  width: 100%;
  min-height: 80px;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
`

const Button = styled.button`
  background-color: #007bff;
  color: #fff;
  padding: 8px 14px;
  border-radius: 4px;
  border: none;
  cursor: pointer;
`

const AdminSettings: React.FC = () => {
    const handleSave = () => {
        alert('Settings saved!')
        // In real app, you'd call an API to save settings
    }

    return (
        <AdminSettingsContainer>
            <h1>Admin Settings</h1>
            <FormGroup>
                <Label>Global Pickup Window (hours)</Label>
                <Input type="number" placeholder="24" />
            </FormGroup>
            <FormGroup>
                <Label>Maintenance Notification Email</Label>
                <Input type="email" placeholder="maintenance@yourdomain.com" />
            </FormGroup>
            <FormGroup>
                <Label>System Announcement</Label>
                <TextArea placeholder="Any global announcements..." />
            </FormGroup>
            <Button onClick={handleSave}>Save</Button>
        </AdminSettingsContainer>
    )
}

export default AdminSettings
