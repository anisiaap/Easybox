import { Stack, usePathname, useRouter } from 'expo-router';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { AuthProvider } from '../lib/AuthContext';
import { NotificationProvider } from '../components/NotificationContext';
export default function Layout() {
    const pathname = usePathname();
    const router = useRouter();

    const isActive = (path: string) => pathname === path;
    const hideNavbar = pathname === '/login' || pathname === '/signup';
    return (
        <NotificationProvider>
        <AuthProvider>
        <View style={{ flex: 1 }}>
            <Stack screenOptions={{ headerShown: false }} />
            {!hideNavbar && (
                <View style={styles.navbar}>
                <NavItem
                    icon="home-outline"
                    label="Orders"
                    route="/home"
                    active={isActive('/home')}
                    onPress={() => router.replace('/home')}
                />
                <NavItem
                    icon="account-circle-outline"
                    label="Account"
                    route="/account"
                    active={isActive('/account')}
                    onPress={() => router.replace('/account')}
                />
            </View>
            )}
        </View>
            </AuthProvider>
            </NotificationProvider>
    );
}

type AllowedIcon = 'home-outline' | 'account-circle-outline';

function NavItem({
                     icon,
                     label,
                     route,
                     active,
                     onPress,
                 }: {
    icon: AllowedIcon;
    label: string;
    route: string;
    active: boolean;
    onPress: () => void;
}) {
    return (
        <Pressable onPress={onPress} style={styles.navItem}>
            <MaterialCommunityIcons
                name={icon}
                size={24}
                color={active ? '#6200ee' : '#888'}
            />
            <Text style={{ color: active ? '#6200ee' : '#888', fontSize: 12 }}>{label}</Text>
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
    navItem: {
        alignItems: 'center',
        flex: 1,
    },
});
