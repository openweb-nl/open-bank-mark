(ns open-bank.db)

(def default-db
  {:selected-nav          :home
   :mob-expand            false
   :show-left             true
   :all-accounts          nil
   :company-iban          "NL66OPEN0000000000"
   :employee-iban         nil
   :transactions          nil
   :subscription-id       0
   :active-t-subscription nil
   :max-items             10
   :login-status          {:valid false}
   :deposit-data          nil
   :transfer-data         {:valid false}
   :show-arguments        {:show_iban        true
                           :show_new_balance true
                           :show_direction   true
                           :show_from_to     true
                           :show_changed_by  true
                           :show_descr       true
                           }})
