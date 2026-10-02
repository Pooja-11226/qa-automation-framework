/** User-facing copy the tests assert on, kept in one place so a wording change is a one-line fix. */
export const loginErrors = {
  usernameRequired: 'Epic sadface: Username is required',
  passwordRequired: 'Epic sadface: Password is required',
  invalidCredentials: 'Epic sadface: Username and password do not match any user in this service',
  lockedOut: 'Epic sadface: Sorry, this user has been locked out.',
  protectedPage: (path: string): string =>
    `Epic sadface: You can only access '${path}' when you are logged in.`,
} as const;

export const checkoutErrors = {
  firstNameRequired: 'Error: First Name is required',
  lastNameRequired: 'Error: Last Name is required',
  postalCodeRequired: 'Error: Postal Code is required',
} as const;

export const pageTitles = {
  inventory: 'Products',
  cart: 'Your Cart',
  checkoutInformation: 'Checkout: Your Information',
  checkoutOverview: 'Checkout: Overview',
  checkoutComplete: 'Checkout: Complete!',
} as const;

export const orderDetails = {
  paymentInformation: 'SauceCard #31337',
  shippingInformation: 'Free Pony Express Delivery!',
  confirmationHeader: 'Thank you for your order!',
} as const;
