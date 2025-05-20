import React, { useState } from 'react';
import {
    View,
    Alert,
    KeyboardAvoidingView,
    Platform,
    StyleSheet,
} from 'react-native';
import {TextInput, Button, Text, Card, Title} from 'react-native-paper';
import { useRouter } from 'expo-router';
import api from '../lib/api';
import { saveToken } from '../lib/auth';
import { useAuth } from '../lib/AuthContext';
import { useNotification } from '../components/NotificationContext';
const PHONE_REGEX = /^07\d{8}$/;


export default function Login() {
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState<'client' | 'bakery'>('client');
    const router = useRouter();
    const { setAuth } = useAuth();
    const [loading, setLoading] = useState(false);
    const { notify } = useNotification();
    const isValid = PHONE_REGEX.test(phone) && password.length > 0;

    const handleLogin = async () => {
        const mappedRole = role === 'client' ? 'USER' : 'BAKERY';
        try {
            setLoading(true);
            const { data: token } = await api.post<string>('/auth/login', {
                phone,
                password,
                role: mappedRole,
            });
            await saveToken(token); // ✱ CHANGE: removed duplicate

            const profile = await api.get('/auth/me').then(r => r.data);


            setAuth({
                userId: profile.userId,
                name: profile.name,
                phone: profile.phone,
                role: profile.role,
                token
            });
            setPassword('');
            router.replace({ pathname: '/redirect', params: { role } });
        } catch (e: any) {
            const msg =
                e?.response?.data?.message ||
                e?.response?.data ||
                'Login failed. Check your phone and password.';
                notify({ type: 'error', message: msg });
        }
        finally{
            setLoading(false);
        }
    };


    return (
        <KeyboardAvoidingView
            behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            style={{ flex: 1, justifyContent: 'center', padding: 20, backgroundColor: '#f4f4f4' }}
        >
            <Card style={{ padding: 20, borderRadius: 16, elevation: 4 }}>
                <Title style={{ textAlign: 'center', marginBottom: 20 }}>Login</Title>

                <TextInput
                    label="Phone Number"
                    value={phone}
                    onChangeText={setPhone}
                    keyboardType="phone-pad"
                    autoCapitalize="none"
                    style={{ marginBottom: 16 }}
                    mode="outlined"
                    error={phone.length > 0 && !PHONE_REGEX.test(phone)}
                />
                <TextInput
                    label="Password"
                    secureTextEntry
                    value={password}
                    onChangeText={setPassword}
                    style={{ marginBottom: 16 }}
                    mode="outlined"
                />

                <View style={{
                    flexDirection: 'row',
                    justifyContent: 'space-between',
                    marginBottom: 22,
                    height: 44,
                    borderColor: 'red',
                }}>
                    <>
                    <Button
                        mode={role === 'client' ? 'contained' : 'outlined'}
                        onPress={() => setRole('client')}
                        style={styles.toggleBtn}
                        accessibilityState={{ selected: role === 'client' }}
                    >
                        Client
                    </Button>
                    <Button
                        mode={role === 'bakery' ? 'contained' : 'outlined'}
                        onPress={() => setRole('bakery')}
                        style={styles.toggleBtn}
                        accessibilityState={{ selected: role === 'bakery' }} // ✱ CHANGE
                    >
                        Bakery
                    </Button>
                    </>
                </View>

                <Button mode="contained" onPress={handleLogin} style={{ borderRadius: 8 }}>
                    Login
                </Button>

                <Text style={{ marginTop: 20, textAlign: 'center' }}>
                    Don't have an account?{' '}
                    <Text style={{ color: '#2196f3' }} onPress={() => router.push('/signup')}>
                        Sign up
                    </Text>
                </Text>
            </Card>
        </KeyboardAvoidingView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, justifyContent: 'center', padding: 20 },
    card: { padding: 20, borderRadius: 16, elevation: 4 },
    title: { textAlign: 'center', marginBottom: 20 },
    input: { marginBottom: 16 },
    toggleRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        marginBottom: 22,
        height: 44,
    },
    toggleBtn: { flex: 1, marginHorizontal: 4 },
    submit: { borderRadius: 8 },
    footer: { marginTop: 20, textAlign: 'center' },
});