import React, { useState } from 'react';
import { Alert, View } from 'react-native';
import { TextInput, Button, Text, Card, Title } from 'react-native-paper';
import { useRouter } from 'expo-router';
import api from '../lib/api';

export default function Signup() {
    const [name, setName] = useState('');
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState<'client' | 'bakery'>('client');
    const router = useRouter();

    const handleSignup = async () => {
        try {
            const endpoint = role === 'client' ? '/auth/register-client' : '/auth/register-bakery';

            const payload =
                role === 'client'
                    ? { name, phoneNumber: phone, password }
                    : { name, phone, password, pluginInstalled: false };

            const res = await api.post(endpoint, payload);

            if (res.status === 200) {
                Alert.alert(
                    'Success',
                    role === 'client'
                        ? 'Client account created!'
                        : 'Bakery created. Please wait for approval.'
                );
                router.replace('/login');
            } else {
                Alert.alert('Signup failed', 'Account may already exist.');
            }
        } catch (err) {
            Alert.alert('Error', 'Signup failed. Try again.');
        }
    };

    return (
        <View style={{ flex: 1, justifyContent: 'center', padding: 20, backgroundColor: '#f9f9f9' }}>
            <Card style={{ padding: 20, borderRadius: 16 }}>
                <Title style={{ textAlign: 'center', marginBottom: 20 }}>📱 Sign Up</Title>

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
                    style={{ marginBottom: 12 }}
                    mode="outlined"
                />
                <TextInput
                    label="Password"
                    value={password}
                    onChangeText={setPassword}
                    secureTextEntry
                    style={{ marginBottom: 20 }}
                    mode="outlined"
                />

                <Button
                    mode={role === 'client' ? 'contained' : 'outlined'}
                    onPress={() => setRole('client')}
                    style={{ marginBottom: 8 }}
                >
                    Sign up as Client
                </Button>
                <Button
                    mode={role === 'bakery' ? 'contained' : 'outlined'}
                    onPress={() => setRole('bakery')}
                    style={{ marginBottom: 20 }}
                >
                    Sign up as Bakery
                </Button>

                <Button mode="contained" onPress={handleSignup}>
                    Create Account
                </Button>
            </Card>
        </View>
    );
}
