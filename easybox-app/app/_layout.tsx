import { Stack, Redirect, usePathname, useRouter } from 'expo-router';
import { ActivityIndicator, PaperProvider } from 'react-native-paper';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { AuthProvider, useAuth }      from '../lib/AuthContext';
import { NotificationProvider }       from '../components/NotificationContext';
import { customTheme }                from '@/lib/theme';

/* ──────────────────────────  root providers  ────────────────────────── */
export default function Layout() {
    return (
        <PaperProvider theme={customTheme}>
            <NotificationProvider>
                <AuthProvider>
                    <RootContent />
                </AuthProvider>
            </NotificationProvider>
        </PaperProvider>
    );
}

/* ───────────────────────────  layout body  ──────────────────────────── */
function RootContent() {
    const { user }   = useAuth();          // undefined | null | UserInfo
    const pathname   = usePathname();
    const router     = useRouter();

    const onAuthPage = pathname === '/login' || pathname === '/signup';
    const hideBar    = onAuthPage;
    const isActive   = (p: string) => pathname === p;

    return (
        <View style={{ flex: 1 }}>
            {/* ✅ Navigator is **always** mounted */}
            <Stack screenOptions={{ headerShown: false }} />

            {/* ⏳ Session still loading */}
            {user === undefined && (
                <View style={styles.overlay}>
                    <ActivityIndicator size="large" />
                </View>
            )}

            {/* 🚪 Auth-gate redirects – Navigator is already present */}
            {user === null  && !onAuthPage && <Redirect href="/login" />}
            {user && onAuthPage               && <Redirect href="/home"  />}

            {/* 📱 Bottom navbar (only after auth pages) */}
            {!hideBar && (
                <View style={styles.navbar}>
                    <NavItem
                        icon="home-outline" label="Orders" route="/home"
                        active={isActive('/home')}
                        onPress={() => router.replace('/home')}
                    />
                    <NavItem
                        icon="account-circle-outline" label="Account" route="/account"
                        active={isActive('/account')}
                        onPress={() => router.replace('/account')}
                    />
                </View>
            )}
        </View>
    );
}

/* ─────────────────────────── helpers / styles ───────────────────────── */
type IconName = 'home-outline' | 'account-circle-outline';

function NavItem({ icon, label, route, active, onPress }: {
    icon: IconName; label: string; route: string;
    active: boolean; onPress: () => void;
}) {
    return (
        <Pressable onPress={onPress} style={styles.navItem}>
            <MaterialCommunityIcons
                name={icon} size={24}
                color={active ? '#6200ee' : '#888'}
            />
            <Text style={{ color: active ? '#6200ee' : '#888', fontSize: 12 }}>
                {label}
            </Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    navbar: {
        flexDirection: 'row',
        justifyContent: 'space-around',
        borderTopWidth: 1,
        borderTopColor: '#ccc',
        backgroundColor: '#fff',
        paddingVertical: 10,
    },
    navItem: { alignItems: 'center', flex: 1 },
    overlay: {
        ...StyleSheet.absoluteFillObject,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'rgba(255,255,255,0.6)',
    },
});