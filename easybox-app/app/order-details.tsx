import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, View, StyleSheet, ScrollView, Image } from 'react-native';
import {Text, Card, Divider, Button, Portal, Dialog, Paragraph} from 'react-native-paper';
import api from '../lib/api';
import ScreenHeader from './ScreenHeader';
import { useAuth } from '../lib/AuthContext';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNotification } from '../components/NotificationContext';
type Order = {
    id: string;
    status: string;
    deliveryTime: string;
    easyboxAddress: string;
    qrCodeData?: string;
    compartmentId: number;
    actionDeadline: string; //
};
function calculateTimeLeft(deadline: string): string {
    const now = new Date();
    const end = new Date(deadline);
    const diff = end.getTime() - now.getTime();

    if (diff <= 0) return 'Expired';

    const minutes = Math.floor((diff / 1000 / 60) % 60);
    const hours = Math.floor((diff / 1000 / 60 / 60) % 24);
    const days = Math.floor(diff / 1000 / 60 / 60 / 24);

    const parts = [];
    if (days > 0) parts.push(`${days}d`);
    if (hours > 0) parts.push(`${hours}h`);
    parts.push(`${minutes}m`);

    return parts.join(' ');
}



export default function OrderDetails() {
    const { id } = useLocalSearchParams();
    const { user } = useAuth();
    const role = user?.role;
    const [timeLeft, setTimeLeft] = useState<string | null>(null);
    const { notify } = useNotification();
    const [confirm, setConfirm] = useState<null | { type: 'dirty' | 'broken' }>(null);
    const [order, setOrder] = useState<Order | null>(null);

    useEffect(() => {
        if (!id || !role) return;

        api.get(`/orders/${id}`)
            .then(res => setOrder(res.data))
            .catch(() => notify({type: 'error', message: "Could not load order."}));
    }, [id, role]);
    useEffect(() => {
        if (!order?.actionDeadline) return;

        const update = () => {
            setTimeLeft(calculateTimeLeft(order.actionDeadline));
        };

        update(); // run once immediately
        const interval = setInterval(update, 30000); // update every 30s

        return () => clearInterval(interval); // cleanup
    }, [order?.actionDeadline]);


    if (!order) return <Text style={styles.loading}>Loading...</Text>;

    const showQr =
        (role === 'bakery' && order.status === 'waiting_bakery_drop_off') ||
        (role === 'client' && order.status === 'waiting_client_pick_up');

    const reportIssue = async (type: 'dirty' | 'broken') => {
        const label = type === 'dirty' ? 'dirty' : 'broken';
        try {
            const apiCall = role === 'bakery'
                ? api.post(
                    `/compartments/${order!.compartmentId}/report-and-reevaluate`,
                    null,
                    { params: { issue: type, reservationId: order!.id } }
                )
                : api.post(
                    `/compartments/${order!.compartmentId}/report-condition?issue=${type}`
                );

            await apiCall;
            notify({ type: 'success', message: `Compartment marked as ${label}.` });
        } catch {
            notify({ type: 'error', message: 'Could not report the issue.' });
        }
    };


    return (
        <SafeAreaView style={styles.safeArea}>
            <ScreenHeader title="Order Details" showBack />
            <View style={styles.container}>
                <ScrollView contentContainerStyle={styles.content}>
                    <Card style={styles.card}>
                        <Card.Title title="Order Summary" />
                        <Card.Content>
                            <Text style={styles.label}>Order ID:</Text>
                            <Text>{order.id}</Text>

                            <Text style={styles.label}>Status:</Text>
                            <Text>{formatStatus(order.status)}</Text>

                            <Text style={styles.label}>Delivery Time:</Text>
                            <Text>{formatDate(order.deliveryTime)}</Text>

                            <Text style={styles.label}>Easybox Location:</Text>
                            <Text>{order.easyboxAddress}</Text>
                            <Text style={styles.label}>
                                Time Left to {role === 'bakery' ? 'place order' : 'pick up'}:
                            </Text>
                            <Text style={styles.countdown}>
                                {timeLeft || '...'}
                            </Text>
                            {showQr && order.qrCodeData && (
                                <>
                                    <Divider style={styles.divider} />
                                    <Text style={styles.label}>QR Code:</Text>
                                    <Image
                                        source={{ uri: `data:image/png;base64,${order.qrCodeData}` }}
                                        style={styles.qr}
                                    />
                                </>
                            )}
                        </Card.Content>
                    </Card>
                </ScrollView>

                {showQr && (
                    <View style={styles.bottomButtons}>
                        <Button
                            mode="outlined"
                            onPress={() => reportIssue('dirty')}
                            style={styles.issueBtn}
                            labelStyle={styles.issueBtnLabel}
                        >
                            Compartment is Dirty
                        </Button>
                        <Button
                            mode="outlined"
                            onPress={() => reportIssue('broken')}
                            style={styles.issueBtn}
                            labelStyle={styles.issueBtnLabel}
                        >
                            Compartment is Broken
                        </Button>
                    </View>
                )}
            </View>
            {/* ✱ NEW: Dialog to confirm report */}
            <Portal>
                <Dialog visible={!!confirm} onDismiss={() => setConfirm(null)}>
                    <Dialog.Title>Confirm Report</Dialog.Title>
                    <Dialog.Content>
                        <Paragraph>
                            Are you sure you want to mark the compartment as {confirm?.type}?
                        </Paragraph>
                    </Dialog.Content>
                    <Dialog.Actions>
                        <Button onPress={() => setConfirm(null)}>Cancel</Button>
                        <Button onPress={() => confirm && reportIssue(confirm.type)}>
                            Yes, report
                        </Button>
                    </Dialog.Actions>
                </Dialog>
            </Portal>
        </SafeAreaView>
    );
}

function formatStatus(status: string) {
    return status.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleString();
}

const styles = StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: '#fff' },
    container: { flex: 1, justifyContent: 'space-between' },
    content: { padding: 20, paddingBottom: 100 },
    card: { borderRadius: 10, elevation: 3, backgroundColor: '#fff' },
    label: { fontWeight: 'bold', marginTop: 12 },
    divider: { marginVertical: 12 },
    qr: { width: 200, height: 200, alignSelf: 'center', marginVertical: 10 },
    bottomButtons: {
        padding: 16,
        backgroundColor: '#fff',
        borderTopWidth: 1,
        borderColor: '#eee',
    },
    issueBtn: {
        marginBottom: 12,
        borderRadius: 8,
    },
    issueBtnLabel: {
        fontSize: 16,
    },
    loading: { marginTop: 50, textAlign: 'center' },
    countdown: {
        fontSize: 20,
        fontWeight: 'bold',
        color: '#d32f2f',
        marginBottom: 12,
    }

});
