import React, { useState } from 'react';
import { Alert, View } from 'react-native';
import { TextInput, Button, Text, Card, Title, HelperText } from 'react-native-paper';
import { useRouter } from 'expo-router';
import api from '../lib/api';

export default function Signup() {
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState<'client' | 'bakery'>('client');
    const [loading, setLoading] = useState(false);
    const router = useRouter();

    const isValidPhone = /^07\d{8}$/.test(phone);
    const isStrongPassword = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d])[A-Za-z\d\S]{8,}$/.test(password);
    const isFormValid = name.trim() !== '' && isValidPhone && isStrongPassword;

    const handleSignup = async () => {
        if (!isFormValid) {
            Alert.alert('Invalid input', 'Please correct the errors before submitting.');
            return;
        }

        try {
            setLoading(true);
            const endpoint = role === 'client' ? '/auth/register-client' : '/auth/register-bakery';
            const payload =
                role === 'client'
                    ? { name, phone, password }
                    : { name, phone, password, pluginInstalled: false };

            const res = await api.post(endpoint, payload);

            Alert.alert(
                'Success',
                role === 'client'
                    ? 'Client account created!'
                    : 'Bakery created. Please wait for approval.'
            );
            router.replace('/login');
        } catch (err: any) {
            const msg =
                err?.response?.data?.message ??
                err?.response?.data ??
                'Signup failed. Try again.';
            Alert.alert('Error', msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <View style={{ flex: 1, justifyContent: 'center', padding: 20, backgroundColor: '#f9f9f9' }}>
            <Card style={{ padding: 20, borderRadius: 16 }}>
                <Title style={{ textAlign: 'center', marginBottom: 20 }}>Sign Up</Title>

                <TextInput
                    label="Name"
                    value={name}
                    onChangeText={setName}
                    style={{ marginBottom: 12 }}
                    mode="outlined"
                />

                <TextInput
                    label="Phone Number"
                    value={phone}
                    onChangeText={setPhone}
                    keyboardType="phone-pad"
                    mode="outlined"
                />
                <HelperText type="error" visible={phone.length > 0 && !isValidPhone}>
                    Phone must start with 07 and be 10 digits
                </HelperText>

                <TextInput
                    label="Password"
                    value={password}
                    onChangeText={setPassword}
                    secureTextEntry
                    mode="outlined"
                />
                <HelperText type="error" visible={password.length > 0 && !isStrongPassword}>
                    Password must be at least 8 characters and contain letters and numbers and a special character
                </HelperText>

                <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 }}>
                    <Button
                        mode={role === 'client' ? 'contained' : 'outlined'}
                        onPress={() => setRole('client')}
                        style={{ flex: 1, marginRight: 8 }}
                    >
                        Client
                    </Button>
                    <Button
                        mode={role === 'bakery' ? 'contained' : 'outlined'}
                        onPress={() => setRole('bakery')}
                        style={{ flex: 1 }}
                    >
                        Bakery
                    </Button>
                </View>

                <Button
                    mode="contained"
                    onPress={handleSignup}
                    style={{ marginTop: 20 }}
                    disabled={!isFormValid || loading}
                    loading={loading}
                >
                    Create Account
                </Button>
            </Card>
        </View>
    );
}
