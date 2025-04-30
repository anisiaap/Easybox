import { useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, View, StyleSheet, ScrollView, Image } from 'react-native';
import { Text, Card, Divider, Button } from 'react-native-paper';
import api from '../lib/api';
import ScreenHeader from './ScreenHeader';
import { useAuth } from '../lib/AuthContext';
import { SafeAreaView } from 'react-native-safe-area-context';

type Order = {
    id: string;
    status: string;
    deliveryTime: string;
    easyboxAddress: string;
    qrCodeData?: string;
    compartmentId: number;
};

export default function OrderDetails() {
    const { id } = useLocalSearchParams();
    const { role } = useAuth();
    const [order, setOrder] = useState<Order | null>(null);

    useEffect(() => {
        api.get(`/orders/${id}`, { params: { role } })
            .then(res => setOrder(res.data))
            .catch(() => Alert.alert("Error", "Could not load order."));
    }, [id, role]);

    if (!order) return <Text style={styles.loading}>Loading...</Text>;

    const showQr =
        (role === 'bakery' && order.status === 'waiting_bakery_drop_off') ||
        (role === 'client' && order.status === 'waiting_client_pick_up');

    const reportIssue = (type: 'dirty' | 'broken') => {
        const label = type === 'dirty' ? 'dirty' : 'broken';
        Alert.alert(
            'Confirm Report',
            `Are you sure you want to mark the compartment as ${label}?`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Yes, report',
                    style: 'destructive',
                    onPress: () => {
                        const apiCall = role === 'bakery'
                            ? api.post(
                                `/compartments/${order!.compartmentId}/report-and-reevaluate`,
                                null,
                                { params: { issue: type, reservationId: order!.id } }
                            )
                            : api.post(
                                `/compartments/${order!.compartmentId}/report-condition?issue=${type}`
                            );

                        apiCall
                            .then(() => Alert.alert('Success', `Compartment marked as ${label}.`))
                            .catch(() => Alert.alert('Error', 'Could not report the issue.'));

                    },
                },
            ]
        );
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
});
