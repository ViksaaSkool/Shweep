import Foundation
import StoreKit

@objc public class StoreKitPurchaseGatewaySwift: NSObject {
    private var onProductLoaded: ((String) -> Void)?
    private var onProductUnavailable: (() -> Void)?
    private var onEntitlementChanged: ((Bool) -> Void)?
    private var onPurchasePending: (() -> Void)?
    private var onPurchaseCancelled: (() -> Void)?
    private var onPurchaseFailed: ((String) -> Void)?
    private var onRestoreCompleted: ((Bool) -> Void)?

    @objc public func setCallbacks(
        productLoaded: @escaping (String) -> Void,
        productUnavailable: @escaping () -> Void,
        entitlementChanged: @escaping (Bool) -> Void,
        purchasePending: @escaping () -> Void,
        purchaseCancelled: @escaping () -> Void,
        purchaseFailed: @escaping (String) -> Void,
        restoreCompleted: @escaping (Bool) -> Void
    ) {
        self.onProductLoaded = productLoaded
        self.onProductUnavailable = productUnavailable
        self.onEntitlementChanged = entitlementChanged
        self.onPurchasePending = purchasePending
        self.onPurchaseCancelled = purchaseCancelled
        self.onPurchaseFailed = purchaseFailed
        self.onRestoreCompleted = restoreCompleted
    }

    @objc public func loadProduct() async {
        do {
            let products = try await Product.products(for: ["unlimited_sheep"])
            if let product = products.first {
                let price = product.displayPrice
                await MainActor.run { self.onProductLoaded?(price) }
            } else {
                await MainActor.run { self.onProductUnavailable?() }
            }
        } catch {
            await MainActor.run { self.onProductUnavailable?() }
        }
    }

    @objc public func checkEntitlement() async {
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result {
                if transaction.productID == "unlimited_sheep" && transaction.revocationDate == nil {
                    await MainActor.run { self.onEntitlementChanged?(true) }
                    return
                }
            }
        }
        await MainActor.run { self.onEntitlementChanged?(false) }
    }

    @objc public func purchase() async {
        do {
            let products = try await Product.products(for: ["unlimited_sheep"])
            guard let product = products.first else {
                await MainActor.run { self.onPurchaseFailed?("Product not found") }
                return
            }

            let result = try await product.purchase()

            switch result {
            case .success(let verification):
                if case .verified(let transaction) = verification {
                    await transaction.finish()
                    await MainActor.run { self.onEntitlementChanged?(true) }
                }
            case .pending:
                await MainActor.run { self.onPurchasePending?() }
            case .userCancelled:
                await MainActor.run { self.onPurchaseCancelled?() }
            @unknown default:
                await MainActor.run { self.onPurchaseFailed?("Unknown error") }
            }
        } catch {
            await MainActor.run { self.onPurchaseFailed?(error.localizedDescription) }
        }
    }

    @objc public func restorePurchases() async {
        do {
            try await AppStore.sync()

            for await result in Transaction.currentEntitlements {
                if case .verified(let transaction) = result {
                    if transaction.productID == "unlimited_sheep" && transaction.revocationDate == nil {
                        await MainActor.run { self.onRestoreCompleted?(true) }
                        return
                    }
                }
            }
            await MainActor.run { self.onRestoreCompleted?(false) }
        } catch {
            await MainActor.run { self.onRestoreCompleted?(false) }
        }
    }
}
